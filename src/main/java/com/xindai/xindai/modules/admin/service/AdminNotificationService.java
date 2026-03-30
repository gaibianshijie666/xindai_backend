package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.admin.dto.AdminNotificationQueryDTO;
import com.xindai.xindai.modules.admin.dto.NotificationBroadcastDTO;
import com.xindai.xindai.modules.admin.dto.NotificationTargetedDTO;
import com.xindai.xindai.modules.notification.dto.NotificationVO;

/**
 * Admin notification service interface
 */
public interface AdminNotificationService {

    /**
     * Get system notifications with pagination and filters
     */
    Page<NotificationVO> getNotifications(AdminNotificationQueryDTO query);

    /**
     * Send broadcast notification to all active users
     */
    void sendBroadcast(NotificationBroadcastDTO dto);

    /**
     * Send targeted notification to specific users
     */
    void sendTargeted(NotificationTargetedDTO dto);
}
