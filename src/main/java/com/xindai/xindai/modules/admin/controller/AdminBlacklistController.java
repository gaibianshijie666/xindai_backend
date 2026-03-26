package com.xindai.xindai.modules.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.dto.BlacklistAddDTO;
import com.xindai.xindai.modules.admin.dto.BlacklistQueryDTO;
import com.xindai.xindai.modules.admin.service.AdminBlacklistService;
import com.xindai.xindai.modules.admin.vo.BlacklistVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端黑名单控制器
 */
@Tag(name = "管理端-黑名单管理", description = "管理端黑名单相关接口")
@RestController
@RequestMapping("/api/v1/admin/blacklist")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlacklistController {

    private final AdminBlacklistService adminBlacklistService;

    @Operation(summary = "获取黑名单列表")
    @GetMapping
    public Result<Page<BlacklistVO>> getBlacklistList(BlacklistQueryDTO queryDTO) {
        return Result.success(adminBlacklistService.getBlacklistList(queryDTO));
    }

    @Operation(summary = "添加黑名单")
    @PostMapping
    @OperateLog(module = "黑名单管理", operation = "添加黑名单")
    public Result<Void> addBlacklist(@Valid @RequestBody BlacklistAddDTO addDTO) {
        adminBlacklistService.addBlacklist(addDTO);
        return Result.success();
    }

    @Operation(summary = "移除黑名单")
    @DeleteMapping("/{id}")
    @OperateLog(module = "黑名单管理", operation = "移除黑名单")
    public Result<Void> removeBlacklist(
            @Parameter(description = "黑名单ID") @PathVariable Long id) {
        adminBlacklistService.removeBlacklist(id);
        return Result.success();
    }
}
