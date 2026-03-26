package com.xindai.xindai.listener;

import com.xindai.xindai.common.event.LoanApplicationApprovedEvent;
import com.xindai.xindai.common.event.LoanApplicationSubmittedEvent;
import com.xindai.xindai.common.event.LoanOverdueDetectedEvent;
import com.xindai.xindai.common.event.RepaymentCompletedEvent;
import com.xindai.xindai.common.notification.channel.NotificationMessage;
import com.xindai.xindai.modules.notification.service.NotificationService;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * NotificationEventListener 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationEventListener 单元测试")
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Channel channel;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    private static final Long USER_ID = 100L;
    private static final Long DELIVERY_TAG = 1L;
    private static final String PHONE = "13800138000";

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setPhone(PHONE);
        testUser.setRealName("张三");
    }

    @Nested
    @DisplayName("贷款申请提交事件测试")
    class ApplicationSubmittedTests {

        @Test
        @DisplayName("贷款申请提交事件 - 发送通知并确认消息")
        void onApplicationSubmitted_sendsNotification() throws Exception {
            // Given
            LoanApplicationSubmittedEvent event = new LoanApplicationSubmittedEvent(
                    1L, USER_ID, new BigDecimal("50000"), 12);
            when(userMapper.selectById(USER_ID)).thenReturn(testUser);

            ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

            // When
            notificationEventListener.onApplicationSubmitted(event, channel, DELIVERY_TAG);

            // Then
            verify(notificationService).sendMultiChannel(messageCaptor.capture());
            verify(channel).basicAck(DELIVERY_TAG, false);

            NotificationMessage capturedMessage = messageCaptor.getValue();
            assertEquals(USER_ID, capturedMessage.getUserId());
            assertEquals("BORROWER", capturedMessage.getUserType());
            assertEquals(PHONE, capturedMessage.getTo());
            assertEquals("贷款申请已提交", capturedMessage.getSubject());
            assertTrue(capturedMessage.getContent().contains("50000"));
            assertTrue(capturedMessage.getContent().contains("12"));
        }
    }

    @Nested
    @DisplayName("贷款审批通过事件测试")
    class ApplicationApprovedTests {

        @Test
        @DisplayName("贷款审批通过事件 - 发送通知并确认消息")
        void onApplicationApproved_sendsNotification() throws Exception {
            // Given
            LoanApplicationApprovedEvent event = new LoanApplicationApprovedEvent(
                    1L, USER_ID, new BigDecimal("50000"), new BigDecimal("45000"), "信用良好");
            when(userMapper.selectById(USER_ID)).thenReturn(testUser);

            ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

            // When
            notificationEventListener.onApplicationApproved(event, channel, DELIVERY_TAG);

            // Then
            verify(notificationService).sendMultiChannel(messageCaptor.capture());
            verify(channel).basicAck(DELIVERY_TAG, false);

            NotificationMessage capturedMessage = messageCaptor.getValue();
            assertEquals(USER_ID, capturedMessage.getUserId());
            assertEquals("BORROWER", capturedMessage.getUserType());
            assertEquals(PHONE, capturedMessage.getTo());
            assertEquals("贷款审批通过", capturedMessage.getSubject());
            assertTrue(capturedMessage.getContent().contains("45000"));
            assertTrue(capturedMessage.getContent().contains("请登录系统查看详情"));
        }
    }

    @Nested
    @DisplayName("逾期检测事件测试")
    class OverdueDetectedTests {

        @Test
        @DisplayName("逾期检测事件 - 发送通知并确认消息")
        void onOverdueDetected_sendsNotification() throws Exception {
            // Given
            LoanOverdueDetectedEvent event = new LoanOverdueDetectedEvent(
                    1L, USER_ID, 30, new BigDecimal("10000"));
            when(userMapper.selectById(USER_ID)).thenReturn(testUser);

            ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

            // When
            notificationEventListener.onOverdueDetected(event, channel, DELIVERY_TAG);

            // Then
            verify(notificationService).sendMultiChannel(messageCaptor.capture());
            verify(channel).basicAck(DELIVERY_TAG, false);

            NotificationMessage capturedMessage = messageCaptor.getValue();
            assertEquals(USER_ID, capturedMessage.getUserId());
            assertEquals("BORROWER", capturedMessage.getUserType());
            assertEquals(PHONE, capturedMessage.getTo());
            assertEquals("逾期还款通知", capturedMessage.getSubject());
            assertTrue(capturedMessage.getContent().contains("30"));
            assertTrue(capturedMessage.getContent().contains("10000"));
        }
    }

    @Nested
    @DisplayName("还款完成事件测试")
    class RepaymentCompletedTests {

        @Test
        @DisplayName("还款完成事件 - 发送通知并确认消息")
        void onRepaymentCompleted_sendsNotification() throws Exception {
            // Given
            RepaymentCompletedEvent event = new RepaymentCompletedEvent(
                    1L, USER_ID, 3, new BigDecimal("5000"));
            when(userMapper.selectById(USER_ID)).thenReturn(testUser);

            ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

            // When
            notificationEventListener.onRepaymentCompleted(event, channel, DELIVERY_TAG);

            // Then
            verify(notificationService).sendMultiChannel(messageCaptor.capture());
            verify(channel).basicAck(DELIVERY_TAG, false);

            NotificationMessage capturedMessage = messageCaptor.getValue();
            assertEquals(USER_ID, capturedMessage.getUserId());
            assertEquals("BORROWER", capturedMessage.getUserType());
            assertEquals(PHONE, capturedMessage.getTo());
            assertEquals("还款成功", capturedMessage.getSubject());
            assertTrue(capturedMessage.getContent().contains("3"));
            assertTrue(capturedMessage.getContent().contains("5000"));
        }
    }
}
