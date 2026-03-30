package com.xindai.xindai.modules.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.dto.CreditLimitAdjustDTO;
import com.xindai.xindai.modules.admin.dto.EnterpriseQueryDTO;
import com.xindai.xindai.modules.admin.dto.EnterpriseStatusDTO;
import com.xindai.xindai.modules.admin.service.AdminEnterpriseService;
import com.xindai.xindai.modules.admin.vo.AdminEnterpriseDetailVO;
import com.xindai.xindai.modules.admin.vo.AdminEnterpriseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端企业管理控制器
 */
@Tag(name = "管理端-企业管理", description = "管理端企业相关接口")
@RestController
@RequestMapping("/api/v1/admin/enterprises")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEnterpriseController {

    private final AdminEnterpriseService adminEnterpriseService;

    @Operation(summary = "获取企业列表")
    @GetMapping
    @OperateLog(module = "企业管理", operation = "查询企业列表")
    public Result<Page<AdminEnterpriseVO>> getEnterpriseList(EnterpriseQueryDTO queryDTO) {
        return Result.success(adminEnterpriseService.getEnterpriseList(queryDTO));
    }

    @Operation(summary = "获取企业详情")
    @GetMapping("/{id}")
    public Result<AdminEnterpriseDetailVO> getEnterpriseDetail(
            @Parameter(description = "企业ID") @PathVariable Long id) {
        return Result.success(adminEnterpriseService.getEnterpriseDetail(id));
    }

    @Operation(summary = "更新企业状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperateLog(module = "企业管理", operation = "修改企业状态")
    public Result<Void> updateEnterpriseStatus(
            @Parameter(description = "企业ID") @PathVariable Long id,
            @Valid @RequestBody EnterpriseStatusDTO statusDTO) {
        adminEnterpriseService.updateEnterpriseStatus(id, statusDTO);
        return Result.success();
    }

    @Operation(summary = "调整企业授信额度")
    @PutMapping("/{id}/credit-limit")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperateLog(module = "企业管理", operation = "调整授信额度")
    public Result<Void> adjustCreditLimit(
            @Parameter(description = "企业ID") @PathVariable Long id,
            @Valid @RequestBody CreditLimitAdjustDTO adjustDTO) {
        adminEnterpriseService.adjustCreditLimit(id, adjustDTO);
        return Result.success();
    }
}
