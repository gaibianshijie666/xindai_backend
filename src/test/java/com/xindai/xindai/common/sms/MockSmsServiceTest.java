package com.xindai.xindai.common.sms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MockSmsService 单元测试
 */
@DisplayName("MockSmsService 单元测试")
class MockSmsServiceTest {

    private MockSmsServiceImpl mockSmsService;

    @BeforeEach
    void setUp() {
        mockSmsService = new MockSmsServiceImpl();
    }

    @Nested
    @DisplayName("发送短信测试")
    class SendTests {

        @Test
        @DisplayName("发送短信 - 返回成功结果")
        void send_returnsSuccess() {
            // Given
            String phone = "13800138000";
            String templateCode = "SMS_VERIFY_CODE";
            Map<String, String> params = Map.of("code", "123456");

            // When
            SendResult result = mockSmsService.send(phone, templateCode, params);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("发送短信 - 生成唯一的messageId")
        void send_generatesMessageId() {
            // Given
            String phone = "13800138000";
            String templateCode = "SMS_VERIFY_CODE";
            Map<String, String> params = Map.of("code", "123456");

            // When
            SendResult result = mockSmsService.send(phone, templateCode, params);

            // Then
            assertNotNull(result.getMessageId());
            assertFalse(result.getMessageId().isEmpty());

            // 多次调用应产生不同的messageId
            SendResult result2 = mockSmsService.send(phone, templateCode, params);
            assertNotEquals(result.getMessageId(), result2.getMessageId());
        }

        @Test
        @DisplayName("发送短信 - 携带参数返回正确消息内容")
        void send_withParams_returnsCorrectMessage() {
            // Given
            String phone = "13800138000";
            String templateCode = "SMS_LOAN_APPROVED";
            Map<String, String> params = Map.of("amount", "50000", "term", "12");

            // When
            SendResult result = mockSmsService.send(phone, templateCode, params);

            // Then
            assertNotNull(result);
            assertEquals("Mock SMS sent successfully", result.getMessage());
            assertTrue(result.isSuccess());
            assertNotNull(result.getMessageId());
        }

        @Test
        @DisplayName("发送短信 - 无参数也返回成功")
        void send_withoutParams_returnsSuccess() {
            // Given
            String phone = "13800138000";
            String templateCode = "SMS_SIMPLE";

            // When
            SendResult result = mockSmsService.send(phone, templateCode, null);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertNotNull(result.getMessageId());
            assertEquals("Mock SMS sent successfully", result.getMessage());
        }
    }
}
