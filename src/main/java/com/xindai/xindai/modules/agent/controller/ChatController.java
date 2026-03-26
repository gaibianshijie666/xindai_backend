package com.xindai.xindai.modules.agent.controller;

import com.xindai.xindai.modules.agent.service.*;
import com.xindai.xindai.modules.agent.tools.ToolContext;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Tag(name = "AI 智能助手", description = "AI对话、业务查询助手接口")
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final UserChatAgent userChatAgent;
    private final EnterpriseChatAgent enterpriseChatAgent;
    private final AdminChatAgent adminChatAgent;
    private final ChatMemoryService chatMemoryService;

    private final ExecutorService sseExecutor = new ThreadPoolExecutor(
            2, 20, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Operation(summary = "用户端流式对话")
    @GetMapping(value = "/user/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter userStreamChat(
            @RequestAttribute("userId") Long userId,
            @RequestParam @NotBlank(message = "消息内容不能为空") @Size(max = 2000, message = "消息长度不能超过2000个字符") String message) {
        ToolContext.setUserId(userId);
        ToolContext.setPortal("user");
        String memoryId = "user:" + userId;
        Map<String, Object> variables = Map.of("current_time", LocalDateTime.now().format(TIME_FORMATTER));
        return createSseEmitter(memoryId, message, variables, (mid, msg, handler) -> {
            userChatAgent.chat(mid, msg, handler);
        });
    }

    @Operation(summary = "企业端流式对话")
    @GetMapping(value = "/enterprise/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter enterpriseStreamChat(
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @RequestParam @NotBlank(message = "消息内容不能为空") @Size(max = 2000, message = "消息长度不能超过2000个字符") String message) {
        ToolContext.setUserId(userId);
        ToolContext.setEnterpriseId(enterpriseId);
        ToolContext.setPortal("enterprise");
        String memoryId = "enterprise:" + enterpriseId;
        Map<String, Object> variables = Map.of("current_time", LocalDateTime.now().format(TIME_FORMATTER));
        return createSseEmitter(memoryId, message, variables, (mid, msg, handler) -> {
            enterpriseChatAgent.chat(mid, msg, handler);
        });
    }

    @Operation(summary = "管理端流式对话")
    @GetMapping(value = "/admin/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter adminStreamChat(
            @RequestAttribute("userId") Long userId,
            @RequestParam @NotBlank(message = "消息内容不能为空") @Size(max = 2000, message = "消息长度不能超过2000个字符") String message) {
        ToolContext.setUserId(userId);
        ToolContext.setPortal("admin");
        String memoryId = "admin:" + userId;
        Map<String, Object> variables = Map.of("current_time", LocalDateTime.now().format(TIME_FORMATTER));
        return createSseEmitter(memoryId, message, variables, (mid, msg, handler) -> {
            adminChatAgent.chat(mid, msg, handler);
        });
    }

    @Operation(summary = "清除聊天历史")
    @DeleteMapping("/{portal}/chat/history")
    public com.xindai.xindai.common.result.Result<Void> clearHistory(
            @RequestAttribute("userId") Long userId,
            @PathVariable String portal) {
        String memoryId = portal + ":" + userId;
        chatMemoryService.clear(memoryId);
        return com.xindai.xindai.common.result.Result.success();
    }

    @FunctionalInterface
    private interface ChatInvoker {
        void invoke(String memoryId, String message, StreamingResponseHandler<AiMessage> handler);
    }

    private SseEmitter createSseEmitter(String memoryId, String message,
                                         Map<String, Object> variables,
                                         ChatInvoker invoker) {
        SseEmitter emitter = new SseEmitter(120_000L);

        sseExecutor.submit(() -> {
            try {
                StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<>() {
                    @Override
                    public void onNext(String token) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data("{\"type\":\"text\",\"content\":\"" + escapeJson(token) + "\"}"));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data("{\"type\":\"done\"}"));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        } finally {
                            ToolContext.clear();
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("Streaming chat error for {}: {}", memoryId, error.getMessage());
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("{\"type\":\"error\",\"content\":\"" + escapeJson(error.getMessage()) + "\"}"));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        } finally {
                            ToolContext.clear();
                        }
                    }
                };

                invoker.invoke(memoryId, message, handler);
            } catch (Exception e) {
                log.error("SSE chat error for {}", memoryId, e);
                emitter.completeWithError(e);
                ToolContext.clear();
            }
        });

        emitter.onTimeout(() -> {
            log.warn("SSE emitter timeout for {}", memoryId);
            ToolContext.clear();
        });

        return emitter;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @PreDestroy
    public void shutdown() {
        sseExecutor.shutdown();
        try {
            if (!sseExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                sseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            sseExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
