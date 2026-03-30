package com.xindai.xindai.modules.admin.controller;

import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.dto.ConfigUpdateDTO;
import com.xindai.xindai.modules.admin.service.AdminConfigService;
import com.xindai.xindai.modules.admin.vo.ConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin config controller
 */
@Tag(name = "管理端-系统配置", description = "管理端系统配置接口")
@RestController
@RequestMapping("/api/v1/admin/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigController {

    private final AdminConfigService adminConfigService;

    @Operation(summary = "获取所有系统配置（按分类分组）")
    @GetMapping
    public Result<Map<String, List<ConfigVO>>> getAllConfigs() {
        return Result.success(adminConfigService.getAllConfigs());
    }

    @Operation(summary = "按分类获取系统配置")
    @GetMapping("/{category}")
    public Result<List<ConfigVO>> getConfigsByCategory(
            @Parameter(description = "分类: LOAN/RISK/COLLECTION/NOTIFICATION")
            @PathVariable String category) {
        return Result.success(adminConfigService.getConfigsByCategory(category));
    }

    @Operation(summary = "更新系统配置（SUPER_ADMIN only）")
    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperateLog(module = "系统配置", operation = "更新系统配置")
    public Result<Void> updateConfigs(@Valid @RequestBody ConfigUpdateDTO updateDTO) {
        adminConfigService.updateConfigs(updateDTO);
        return Result.success("配置更新成功");
    }
}
