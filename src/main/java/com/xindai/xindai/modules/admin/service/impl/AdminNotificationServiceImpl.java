package com.xindai.xindai.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.admin.dto.AdminNotificationQueryDTO;
import com.xindai.xindai.modules.admin.dto.NotificationBroadcastDTO;
import com.xindai.xindai.modules.admin.dto.NotificationTargetedDTO;
import com.xindai.xindai.modules.admin.service.AdminNotificationService;
import com.xindai.xindai.modules.notification.dto.NotificationVO;
import com.xindai.xindai.modules.notification.entity.Notification;
import com.xindai.xindai.modules.notification.mapper.NotificationMapper;
import com.xindai.xindai.modules.notification.service.NotificationService;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin notification service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    @Override
    public Page<NotificationVO> getNotifications(AdminNotificationQueryDTO query) {
        Page<Notification> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();

        if (query.getType() != null && !query.getType().isEmpty()) {
            wrapper.eq(Notification::getType, query.getType());
        }
        if (query.getIsRead() != null) {
            wrapper.eq(Notification::getIsRead, query.getIsRead());
        }

        wrapper.orderByDesc(Notification::getCreatedAt);

        Page<Notification> entityPage = notificationMapper.selectPage(page, wrapper);

        Page<NotificationVO> voPage = new Page<>(
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal()
        );

        List<NotificationVO> voList = entityPage.getRecords().stream()
                .map(n -> {
                    NotificationVO vo = new NotificationVO();
                    BeanUtils.copyProperties(n, vo);
                    vo.setIsRead(n.getIsRead() != null && n.getIsRead() == 1);
                    return vo;
                })
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendBroadcast(NotificationBroadcastDTO dto) {
        // Get all active users
        List<User> activeUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
        );

        if (activeUsers.isEmpty()) {
            log.warn("No active users found for broadcast notification");
            return;
        }

        // Create notifications for all active users
        List<Notification> notifications = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (User user : activeUsers) {
            Notification notification = new Notification();
            notification.setUserId(user.getId());
            notification.setUserType("USER");
            notification.setTitle(dto.getTitle());
            notification.setContent(dto.getContent());
            notification.setType(dto.getType());
            notification.setIsRead(0);
            notification.setCreatedAt(now);
            notifications.add(notification);
        }

        // Batch insert
        for (Notification notification : notifications) {
            notificationMapper.insert(notification);
        }

        log.info("Broadcast notification sent to {} active users", activeUsers.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendTargeted(NotificationTargetedDTO dto) {
        LocalDateTime now = LocalDateTime.now();

        for (Long userId : dto.getUserIds()) {
            // Verify user exists
            User user = userMapper.selectById(userId);
            if (user == null || user.getStatus() != 1) {
                log.warn("User not found or inactive: {}", userId);
                continue;
            }

            notificationService.send(
                    userId,
                    "USER",
                    dto.getTitle(),
                    dto.getContent(),
                    dto.getType(),
                    null
            );
        }

        log.info("Targeted notification sent to {} users", dto.getUserIds().size());
    }
}
