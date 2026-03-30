package com.xindai.xindai.modules.enterprise.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.enterprise.dto.*;
import com.xindai.xindai.modules.enterprise.service.EnterpriseLoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业借款管理控制器
 */
@Tag(name = "企业借款管理", description = "代客借款申请接口")
@RestController
@RequestMapping("/api/v1/enterprise/loans")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ENTERPRISE_ADMIN', 'ENTERPRISE_OPERATOR')")
public class EnterpriseLoanController {

    private final EnterpriseLoanService loanService;

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For (take the first one)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Operation(summary = "获取借款列表")
    @GetMapping
    public Result<Page<EnterpriseLoanVO>> list(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            EnterpriseLoanQueryDTO queryDTO) {
        return Result.success(loanService.list(enterpriseId, queryDTO));
    }

    @Operation(summary = "代客申请借款")
    @PostMapping("/apply")
    @OperateLog(module = "企业借款管理", operation = "代客申请借款")
    public Result<EnterpriseLoanVO> apply(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody EnterpriseLoanApplyDTO dto,
            HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        return Result.success(loanService.apply(enterpriseId, userId, dto, ipAddress));
    }

    @Operation(summary = "获取借款详情")
    @GetMapping("/{id}")
    public Result<EnterpriseLoanVO> getById(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @PathVariable Long id) {
        return Result.success(loanService.getById(enterpriseId, id));
    }

    @Operation(summary = "批量借款申请")
    @PostMapping("/batch-apply")
    @PreAuthorize("hasRole('ENTERPRISE_ADMIN')")
    @OperateLog(module = "企业借款管理", operation = "批量借款申请")
    public Result<BatchOperationResultVO> batchApply(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @RequestAttribute("userId") Long userId,
            @RequestBody List<@Valid EnterpriseLoanApplyDTO> dtos,
            HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        return Result.success(loanService.batchApply(enterpriseId, userId, dtos, ipAddress));
    }

    @Operation(summary = "批量审核借款")
    @PutMapping("/batch-review")
    @PreAuthorize("hasRole('ENTERPRISE_ADMIN')")
    @OperateLog(module = "企业借款管理", operation = "批量审核借款")
    public Result<BatchOperationResultVO> batchReview(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody BatchReviewDTO dto,
            HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        return Result.success(loanService.batchReview(enterpriseId, userId, dto, ipAddress));
    }
}
