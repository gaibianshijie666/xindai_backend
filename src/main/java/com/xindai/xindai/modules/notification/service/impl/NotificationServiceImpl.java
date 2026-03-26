package com.xindai.xindai.modules.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.notification.dto.NotificationQueryDTO;
import com.xindai.xindai.modules.notification.dto.NotificationVO;
import com.xindai.xindai.modules.notification.entity.Notification;
import com.xindai.xindai.modules.notification.mapper.NotificationMapper;
import com.xindai.xindai.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public void send(Long userId, String userType, String title, String content, String type, String relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserType(userType);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        log.info("Notification sent: userId={}, type={}, title={}", userId, type, title);
    }

    @Override
    public Page<NotificationVO> list(Long userId, NotificationQueryDTO query) {
        Page<Notification> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        if (Boolean.TRUE.equals(query.getUnreadOnly())) {
            wrapper.eq(Notification::getIsRead, 0);
        }
        if (query.getType() != null && !query.getType().isEmpty()) {
            wrapper.eq(Notification::getType, query.getType());
        }
        wrapper.orderByDesc(Notification::getCreatedAt);

        Page<Notification> entityPage = notificationMapper.selectPage(page, wrapper);

        Page<NotificationVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<NotificationVO> voList = entityPage.getRecords().stream().map(n -> {
            NotificationVO vo = new NotificationVO();
            BeanUtils.copyProperties(n, vo);
            vo.setIsRead(n.getIsRead() != null && n.getIsRead() == 1);
            return vo;
        }).toList();
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectOne(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getId, notificationId)
                        .eq(Notification::getUserId, userId)
        );
        if (notification != null && (notification.getIsRead() == null || notification.getIsRead() == 0)) {
            notification.setIsRead(1);
            notification.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
    }

    @Override
    public void markAllRead(Long userId) {
        notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
                        .set(Notification::getIsRead, 1)
                        .set(Notification::getReadAt, LocalDateTime.now())
        );
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
        );
    }
}
