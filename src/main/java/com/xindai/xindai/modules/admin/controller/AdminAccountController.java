package com.xindai.xindai.modules.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.dto.AdminCreateDTO;
import com.xindai.xindai.modules.admin.dto.AdminResetPasswordDTO;
import com.xindai.xindai.modules.admin.dto.AdminRoleDTO;
import com.xindai.xindai.modules.admin.dto.UserStatusUpdateDTO;
import com.xindai.xindai.modules.admin.service.AdminAccountService;
import com.xindai.xindai.modules.admin.vo.AdminAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端管理员账号控制器
 */
@Tag(name = "管理端-管理员管理", description = "管理端管理员账号相关接口")
@RestController
@RequestMapping("/api/v1/admin/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @Operation(summary = "获取管理员账号列表")
    @GetMapping
    @OperateLog(module = "管理员管理", operation = "查询管理员列表")
    public Result<Page<AdminAccountVO>> getAdminList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return Result.success(adminAccountService.getAdminList(page, size, keyword));
    }

    @Operation(summary = "创建管理员账号")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperateLog(module = "管理员管理", operation = "创建管理员")
    public Result<Void> createAdmin(@Valid @RequestBody AdminCreateDTO createDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentAdminId = (Long) authentication.getPrincipal();
        adminAccountService.createAdmin(createDTO, currentAdminId);
        return Result.success();
    }

    @Operation(summary = "修改管理员角色")
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperateLog(module = "管理员管理", operation = "修改管理员角色")
    public Result<Void> updateAdminRole(
            @Parameter(description = "管理员ID") @PathVariable Long id,
            @Valid @RequestBody AdminRoleDTO roleDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentAdminId = (Long) authentication.getPrincipal();
        adminAccountService.updateAdminRole(id, roleDTO, currentAdminId);
        return Result.success();
    }

    @Operation(summary = "修改管理员状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperateLog(module = "管理员管理", operation = "修改管理员状态")
    public Result<Void> updateAdminStatus(
            @Parameter(description = "管理员ID") @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateDTO statusDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentAdminId = (Long) authentication.getPrincipal();
        adminAccountService.updateAdminStatus(id, statusDTO, currentAdminId);
        return Result.success();
    }

    @Operation(summary = "重置管理员密码")
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperateLog(module = "管理员管理", operation = "重置管理员密码")
    public Result<Void> resetAdminPassword(
            @Parameter(description = "管理员ID") @PathVariable Long id,
            @Valid @RequestBody AdminResetPasswordDTO resetDTO) {
        adminAccountService.resetAdminPassword(id, resetDTO);
        return Result.success();
    }
}
