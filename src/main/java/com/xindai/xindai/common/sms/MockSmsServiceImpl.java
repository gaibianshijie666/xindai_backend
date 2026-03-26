package com.xindai.xindai.common.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "sms.enabled", havingValue = "false", matchIfMissing = true)
public class MockSmsServiceImpl implements SmsService {

    @Override
    public SendResult send(String phone, String templateCode, Map<String, String> params) {
        String messageId = UUID.randomUUID().toString();
        log.info("[SMS-MOCK] Sending SMS to: {}, templateCode: {}, params: {}, messageId: {}",
                phone, templateCode, params, messageId);
        return SendResult.builder()
                .success(true)
                .messageId(messageId)
                .message("Mock SMS sent successfully")
                .build();
    }
}
