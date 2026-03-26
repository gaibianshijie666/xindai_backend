package com.xindai.xindai.common.notification.channel;

import com.xindai.xindai.modules.notification.service.NotificationService;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationService notificationService;
    private final UserMapper userMapper;

    @Override
    public boolean supports(NotificationType type) {
        // In-app notification supports all types
        return true;
    }

    @Override
    public void send(NotificationMessage message) {
        String userType = message.getUserType();
        if (userType == null || userType.isBlank()) {
            // Default to borrower
            userType = "BORROWER";
        }

        String title = message.getSubject();
        if (title == null || title.isBlank()) {
            title = message.getType().getDesc();
        }

        String content = message.getContent();
        if (content == null || content.isBlank()) {
            content = buildDefaultContent(message);
        }

        notificationService.send(
                message.getUserId(),
                userType,
                title,
                content,
                message.getType().getCode(),
                message.getRelatedId()
        );

        log.info("[IN-APP] Notification saved for userId={}, type={}", message.getUserId(), message.getType());
    }

    private String buildDefaultContent(NotificationMessage message) {
        return "您有一条新的通知：" + message.getType().getDesc();
    }
}
