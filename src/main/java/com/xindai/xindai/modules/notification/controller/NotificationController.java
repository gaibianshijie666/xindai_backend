package com.xindai.xindai.modules.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.notification.dto.NotificationQueryDTO;
import com.xindai.xindai.modules.notification.dto.NotificationVO;
import com.xindai.xindai.modules.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "消息通知")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "查询通知列表")
    public Result<Page<NotificationVO>> list(NotificationQueryDTO query,
                                              @RequestAttribute("userId") Long userId) {
        return Result.success(notificationService.list(userId, query));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量")
    public Result<Long> getUnreadCount(@RequestAttribute("userId") Long userId) {
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知已读")
    public Result<Void> markRead(@PathVariable Long id,
                                 @RequestAttribute("userId") Long userId) {
        notificationService.markRead(userId, id);
        return Result.success();
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public Result<Void> markAllRead(@RequestAttribute("userId") Long userId) {
        notificationService.markAllRead(userId);
        return Result.success();
    }
}
