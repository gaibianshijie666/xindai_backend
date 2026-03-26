package com.xindai.xindai.common.notification.channel;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class NotificationMessage {

    private Long userId;
    private String userType;
    private String to;
    private String subject;
    private String content;
    private NotificationType type;
    private String templateCode;
    private Map<String, String> params;
    private String relatedId;
}
