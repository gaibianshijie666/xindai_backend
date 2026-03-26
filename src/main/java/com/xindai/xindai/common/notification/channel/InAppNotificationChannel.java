package com.xindai.xindai.common.notification.channel;

import com.xindai.xindai.modules.notification.entity.Notification;
import com.xindai.xindai.modules.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationMapper notificationMapper;

    @Override
    public boolean supports(NotificationType type) {
        // In-app notification supports all types
        return true;
    }

    @Override
    public void send(NotificationMessage message) {
        String userType = message.getUserType();
        if (userType == null || userType.isBlank()) {
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

        Notification notification = new Notification();
        notification.setUserId(message.getUserId());
        notification.setUserType(userType);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(message.getType().getCode());
        notification.setRelatedId(message.getRelatedId());
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);

        log.info("[IN-APP] Notification saved for userId={}, type={}", message.getUserId(), message.getType());
    }

    private String buildDefaultContent(NotificationMessage message) {
        return "您有一条新的通知：" + message.getType().getDesc();
    }
}
