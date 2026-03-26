package com.xindai.xindai.modules.enterprise.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseRiskQueryDTO;
import com.xindai.xindai.modules.enterprise.dto.RiskAssessResultVO;
import com.xindai.xindai.modules.enterprise.service.EnterpriseRiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业风控评估控制器
 */
@Tag(name = "企业风控评估", description = "企业客户风险评估接口")
@RestController
@RequestMapping("/api/v1/enterprise/risk")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ENTERPRISE_ADMIN', 'ENTERPRISE_OPERATOR')")
public class EnterpriseRiskController {

    private final EnterpriseRiskService riskService;

    @Operation(summary = "评估客户风险")
    @PostMapping("/assess/{customerId}")
    public Result<RiskAssessResultVO> assess(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @PathVariable Long customerId) {
        return Result.success(riskService.assessCustomer(enterpriseId, customerId));
    }

    @Operation(summary = "批量评估客户风险")
    @PostMapping("/batch-assess")
    public Result<List<RiskAssessResultVO>> batchAssess(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @RequestBody List<Long> customerIds) {
        return Result.success(riskService.batchAssess(enterpriseId, customerIds));
    }

    @Operation(summary = "获取评估历史")
    @GetMapping("/history/{customerId}")
    public Result<Page<RiskAssessResultVO>> getHistory(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @PathVariable Long customerId,
            EnterpriseRiskQueryDTO queryDTO) {
        return Result.success(riskService.getAssessHistory(enterpriseId, customerId, queryDTO));
    }
}
