package com.xindai.xindai.modules.agent.config;

import com.xindai.xindai.modules.agent.service.AdminChatAgent;
import com.xindai.xindai.modules.agent.service.EnterpriseChatAgent;
import com.xindai.xindai.modules.agent.service.UserChatAgent;
import com.xindai.xindai.modules.agent.tools.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentBeansConfig {

    private final StreamingChatLanguageModel streamingChatModel;
    private final ChatMemoryStore chatMemoryStore;
    private final LangChain4jProperties properties;

    private String userSystemPrompt;
    private String enterpriseSystemPrompt;
    private String adminSystemPrompt;

    @PostConstruct
    public void loadPrompts() {
        userSystemPrompt = loadPrompt("prompts/user-prompt.txt");
        enterpriseSystemPrompt = loadPrompt("prompts/enterprise-prompt.txt");
        adminSystemPrompt = loadPrompt("prompts/admin-prompt.txt");
        log.info("AI Agent prompts loaded: user={}, enterprise={}, admin={}",
                userSystemPrompt.length(), enterpriseSystemPrompt.length(), adminSystemPrompt.length());
    }

    private String loadPrompt(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to load prompt from classpath: {}", path, e);
            return "你是信贷平台的智能助手。";
        }
    }

    private ChatMemory buildChatMemory(Object memoryId) {
        return MessageWindowChatMemory.builder()
                .id(memoryId)
                .chatMemoryStore(chatMemoryStore)
                .maxMessages(properties.getMemoryMaxMessages())
                .build();
    }

    @Bean
    public UserChatAgent userChatAgent(UserTools userTools, LoanTools loanTools,
                                       RiskTools riskTools) {
        return AiServices.builder(UserChatAgent.class)
                .streamingChatLanguageModel(streamingChatModel)
                .chatMemoryProvider(this::buildChatMemory)
                .systemMessageProvider(memoryId -> userSystemPrompt)
                .tools(userTools, loanTools, riskTools)
                .build();
    }

    @Bean
    public EnterpriseChatAgent enterpriseChatAgent(EnterpriseTools enterpriseTools,
                                                   LoanTools loanTools, RiskTools riskTools) {
        return AiServices.builder(EnterpriseChatAgent.class)
                .streamingChatLanguageModel(streamingChatModel)
                .chatMemoryProvider(this::buildChatMemory)
                .systemMessageProvider(memoryId -> enterpriseSystemPrompt)
                .tools(enterpriseTools, loanTools, riskTools)
                .build();
    }

    @Bean
    public AdminChatAgent adminChatAgent(AdminTools adminTools, UserTools userTools,
                                          LoanTools loanTools, RiskTools riskTools,
                                          EnterpriseTools enterpriseTools) {
        return AiServices.builder(AdminChatAgent.class)
                .streamingChatLanguageModel(streamingChatModel)
                .chatMemoryProvider(this::buildChatMemory)
                .systemMessageProvider(memoryId -> adminSystemPrompt)
                .tools(adminTools, userTools, loanTools, riskTools, enterpriseTools)
                .build();
    }
}
