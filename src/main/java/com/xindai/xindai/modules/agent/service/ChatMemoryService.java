package com.xindai.xindai.modules.agent.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    private final ChatMemoryStore chatMemoryStore;

    public List<ChatMessage> getHistory(String memoryId) {
        return chatMemoryStore.getMessages(memoryId);
    }

    public void clear(String memoryId) {
        chatMemoryStore.deleteMessages(memoryId);
        log.info("Cleared chat history for memoryId: {}", memoryId);
    }
}
