package com.xindai.xindai.modules.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.dto.UserQueryDTO;
import com.xindai.xindai.modules.admin.dto.UserStatusUpdateDTO;
import com.xindai.xindai.modules.admin.service.AdminUserService;
import com.xindai.xindai.modules.admin.vo.AdminUserDetailVO;
import com.xindai.xindai.modules.admin.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端用户控制器
 */
@Tag(name = "管理端-用户管理", description = "管理端用户相关接口")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "获取用户列表")
    @GetMapping
    @OperateLog(module = "用户管理", operation = "查询用户列表")
    public Result<Page<AdminUserVO>> getUserList(UserQueryDTO queryDTO) {
        return Result.success(adminUserService.getUserList(queryDTO));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<AdminUserDetailVO> getUserDetail(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        return Result.success(adminUserService.getUserDetail(id));
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/{id}/status")
    @OperateLog(module = "用户管理", operation = "修改用户状态")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateDTO updateDTO) {
        adminUserService.updateUserStatus(id, updateDTO);
        return Result.success();
    }
}
