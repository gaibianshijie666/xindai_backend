package com.xindai.xindai.modules.agent.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.data.message.AiMessage;

public interface UserChatAgent {

    void chat(@MemoryId String memoryId, @UserMessage String userMessage,
              StreamingResponseHandler<AiMessage> handler);
}
