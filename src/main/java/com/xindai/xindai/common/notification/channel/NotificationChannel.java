package com.xindai.xindai.common.notification.channel;

public interface NotificationChannel {

    /**
     * Whether this channel supports the given notification type.
     */
    boolean supports(NotificationType type);

    /**
     * Send a notification through this channel.
     */
    void send(NotificationMessage message);
}
