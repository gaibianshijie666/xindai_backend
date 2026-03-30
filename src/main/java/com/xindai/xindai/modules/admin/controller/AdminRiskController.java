package com.xindai.xindai.modules.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.dto.RiskAssessmentQueryDTO;
import com.xindai.xindai.modules.admin.dto.RiskOverrideDTO;
import com.xindai.xindai.modules.admin.dto.RiskRuleUpdateDTO;
import com.xindai.xindai.modules.admin.service.AdminRiskService;
import com.xindai.xindai.modules.admin.vo.AdminRiskAssessmentVO;
import com.xindai.xindai.modules.admin.vo.AdminRiskAssessmentDetailVO;
import com.xindai.xindai.modules.admin.vo.RiskRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端风控管理控制器
 */
@Tag(name = "管理端-风控管理", description = "管理端风控相关接口")
@RestController
@RequestMapping("/api/v1/admin/risk")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRiskController {

    private final AdminRiskService adminRiskService;

    @Operation(summary = "获取风险评估列表")
    @GetMapping("/assessments")
    public Result<Page<AdminRiskAssessmentVO>> getAssessmentList(RiskAssessmentQueryDTO queryDTO) {
        return Result.success(adminRiskService.getAssessmentList(queryDTO));
    }

    @Operation(summary = "获取风险评估详情")
    @GetMapping("/assessments/{id}")
    public Result<AdminRiskAssessmentDetailVO> getAssessmentDetail(
            @Parameter(description = "评估ID") @PathVariable Long id) {
        return Result.success(adminRiskService.getAssessmentDetail(id));
    }

    @Operation(summary = "覆盖风险决策")
    @PostMapping("/assessments/{id}/override")
    @OperateLog(module = "风控管理", operation = "覆盖风险决策")
    public Result<Void> overrideDecision(
            @Parameter(description = "评估ID") @PathVariable Long id,
            @Valid @RequestBody RiskOverrideDTO overrideDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = Long.parseLong(userDetails.getUsername());
        adminRiskService.overrideDecision(id, overrideDTO, adminId);
        return Result.success();
    }

    @Operation(summary = "获取风险规则配置")
    @GetMapping("/rules")
    public Result<RiskRuleVO> getRiskRules() {
        return Result.success(adminRiskService.getRiskRules());
    }

    @Operation(summary = "更新风险规则配置")
    @PutMapping("/rules")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperateLog(module = "风控管理", operation = "更新风险规则")
    public Result<Void> updateRiskRules(@Valid @RequestBody RiskRuleUpdateDTO updateDTO) {
        // 当前只读,保留接口
        RiskRuleVO ruleVO = new RiskRuleVO();
        ruleVO.setAutoApproveThreshold(updateDTO.getAutoApproveThreshold());
        ruleVO.setAutoRejectThreshold(updateDTO.getAutoRejectThreshold());
        adminRiskService.updateRiskRules(ruleVO);
        return Result.success();
    }
}
