package com.xindai.xindai.modules.notification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.notification.channel.NotificationMessage;
import com.xindai.xindai.modules.notification.dto.NotificationQueryDTO;
import com.xindai.xindai.modules.notification.dto.NotificationVO;

public interface NotificationService {
    void send(Long userId, String userType, String title, String content, String type, String relatedId);
    Page<NotificationVO> list(Long userId, NotificationQueryDTO query);
    void markRead(Long userId, Long notificationId);
    void markAllRead(Long userId);
    long getUnreadCount(Long userId);

    /**
     * Send notification through all supported channels.
     */
    void sendMultiChannel(NotificationMessage message);
}
