package com.xindai.xindai.modules.agent;

import com.xindai.xindai.modules.agent.controller.ChatController;
import com.xindai.xindai.modules.agent.service.AdminChatAgent;
import com.xindai.xindai.modules.agent.service.ChatMemoryService;
import com.xindai.xindai.modules.agent.service.EnterpriseChatAgent;
import com.xindai.xindai.modules.agent.service.UserChatAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChatController 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatController 单元测试")
class ChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserChatAgent userChatAgent;

    @Mock
    private EnterpriseChatAgent enterpriseChatAgent;

    @Mock
    private AdminChatAgent adminChatAgent;

    @Mock
    private ChatMemoryService chatMemoryService;

    @InjectMocks
    private ChatController chatController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
    }

    @Nested
    @DisplayName("清除聊天历史测试")
    class ClearHistoryTests {

        @Test
        @DisplayName("清除用户端聊天历史 - 调用chatMemoryService.clear并传入正确memoryId")
        void clearHistory_userPortal_callsChatMemoryServiceClear() throws Exception {
            mockMvc.perform(delete("/api/v1/agent/user/chat/history")
                            .requestAttr("userId", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(chatMemoryService).clear("user:1");
        }

        @Test
        @DisplayName("清除企业端聊天历史 - 调用chatMemoryService.clear并传入正确memoryId")
        void clearHistory_enterprisePortal_callsChatMemoryServiceClear() throws Exception {
            mockMvc.perform(delete("/api/v1/agent/enterprise/chat/history")
                            .requestAttr("userId", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(chatMemoryService).clear("enterprise:100");
        }

        @Test
        @DisplayName("清除管理端聊天历史 - 调用chatMemoryService.clear并传入正确memoryId")
        void clearHistory_adminPortal_callsChatMemoryServiceClear() throws Exception {
            mockMvc.perform(delete("/api/v1/agent/admin/chat/history")
                            .requestAttr("userId", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(chatMemoryService).clear("admin:1");
        }

        @Test
        @DisplayName("清除聊天历史 - 任意portal路径变量正常工作")
        void clearHistory_anyPortal_works() throws Exception {
            mockMvc.perform(delete("/api/v1/agent/custom/chat/history")
                            .requestAttr("userId", 42L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(chatMemoryService).clear("custom:42");
        }
    }

    @Nested
    @DisplayName("SSE流式对话端点验证测试")
    class StreamingEndpointTests {

        @Test
        @DisplayName("用户端流式对话端点存在且返回SSE响应")
        void userStreamChat_endpointExists() {
            var methodResults = java.util.Arrays.stream(ChatController.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals("userStreamChat"))
                    .findFirst();

            assertTrue(methodResults.isPresent());
            assertEquals(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.class,
                    methodResults.get().getReturnType());
        }

        @Test
        @DisplayName("企业端流式对话端点存在且返回SSE响应")
        void enterpriseStreamChat_endpointExists() {
            var methodResults = java.util.Arrays.stream(ChatController.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals("enterpriseStreamChat"))
                    .findFirst();

            assertTrue(methodResults.isPresent());
            assertEquals(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.class,
                    methodResults.get().getReturnType());
        }

        @Test
        @DisplayName("管理端流式对话端点存在且返回SSE响应")
        void adminStreamChat_endpointExists() {
            var methodResults = java.util.Arrays.stream(ChatController.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals("adminStreamChat"))
                    .findFirst();

            assertTrue(methodResults.isPresent());
            assertEquals(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.class,
                    methodResults.get().getReturnType());
        }
    }
}
