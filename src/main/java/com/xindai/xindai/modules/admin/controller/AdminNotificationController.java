package com.xindai.xindai.modules.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.dto.AdminNotificationQueryDTO;
import com.xindai.xindai.modules.admin.dto.NotificationBroadcastDTO;
import com.xindai.xindai.modules.admin.dto.NotificationTargetedDTO;
import com.xindai.xindai.modules.admin.service.AdminNotificationService;
import com.xindai.xindai.modules.notification.dto.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin notification controller
 */
@Tag(name = "管理端-通知管理", description = "管理端通知管理接口")
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @Operation(summary = "获取系统通知列表（分页，支持过滤）")
    @GetMapping
    public Result<Page<NotificationVO>> getNotifications(@Valid AdminNotificationQueryDTO query) {
        return Result.success(adminNotificationService.getNotifications(query));
    }

    @Operation(summary = "发送广播通知（给所有活跃用户）")
    @PostMapping("/broadcast")
    @OperateLog(module = "通知管理", operation = "发送广播通知")
    public Result<Void> sendBroadcast(@Valid @RequestBody NotificationBroadcastDTO dto) {
        adminNotificationService.sendBroadcast(dto);
        return Result.success("广播通知发送成功");
    }

    @Operation(summary = "发送定向通知（给指定用户）")
    @PostMapping("/targeted")
    @OperateLog(module = "通知管理", operation = "发送定向通知")
    public Result<Void> sendTargeted(@Valid @RequestBody NotificationTargetedDTO dto) {
        adminNotificationService.sendTargeted(dto);
        return Result.success("定向通知发送成功");
    }
}
