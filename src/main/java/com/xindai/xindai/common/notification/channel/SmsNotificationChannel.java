package com.xindai.xindai.common.notification.channel;

import com.xindai.xindai.common.sms.SendResult;
import com.xindai.xindai.common.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsNotificationChannel implements NotificationChannel {

    private final SmsService smsService;

    private static final Set<NotificationType> SUPPORTED_TYPES = Set.of(
            NotificationType.LOAN_APPROVED,
            NotificationType.REPAYMENT_REMINDER,
            NotificationType.OVERDUE_NOTICE,
            NotificationType.COLLECTION_NOTICE,
            NotificationType.DISBURSEMENT_COMPLETED
    );

    @Override
    public boolean supports(NotificationType type) {
        return SUPPORTED_TYPES.contains(type);
    }

    @Override
    @Async
    public void send(NotificationMessage message) {
        if (message.getTo() == null || message.getTo().isBlank()) {
            log.warn("[SMS] No phone number provided, skipping. userId={}", message.getUserId());
            return;
        }

        SendResult result = smsService.send(
                message.getTo(),
                message.getTemplateCode() != null ? message.getTemplateCode() : message.getType().getCode(),
                message.getParams()
        );

        if (result.isSuccess()) {
            log.info("[SMS] Sent SMS to: {}, template: {}, messageId: {}",
                    message.getTo(), message.getTemplateCode(), result.getMessageId());
        } else {
            log.warn("[SMS] Failed to send SMS to: {}, template: {}, reason: {}",
                    message.getTo(), message.getTemplateCode(), result.getMessage());
        }
    }
}
