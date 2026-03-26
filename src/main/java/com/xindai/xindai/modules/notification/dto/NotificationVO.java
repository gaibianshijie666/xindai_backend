package com.xindai.xindai.modules.notification.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Long id;
    private String title;
    private String content;
    private String type;
    private Boolean isRead;
    private String relatedId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
