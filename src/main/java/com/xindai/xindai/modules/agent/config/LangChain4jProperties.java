package com.xindai.xindai.modules.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class LangChain4jProperties {

    private boolean enabled = true;
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "sk-placeholder";
    private String modelName = "deepseek-chat";
    private double temperature = 0.7;
    private int maxTokens = 2048;
    private int timeoutSeconds = 60;
    private int memoryMaxMessages = 20;
}
