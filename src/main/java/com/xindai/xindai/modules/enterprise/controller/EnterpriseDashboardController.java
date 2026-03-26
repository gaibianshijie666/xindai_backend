package com.xindai.xindai.modules.enterprise.controller;

import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.enterprise.dto.DashboardOverviewVO;
import com.xindai.xindai.modules.enterprise.dto.DashboardTrendVO;
import com.xindai.xindai.modules.enterprise.service.EnterpriseDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 企业数据看板控制器
 */
@Tag(name = "企业数据看板", description = "企业数据统计接口")
@RestController
@RequestMapping("/api/v1/enterprise/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ENTERPRISE_ADMIN', 'ENTERPRISE_OPERATOR')")
public class EnterpriseDashboardController {

    private final EnterpriseDashboardService dashboardService;

    @Operation(summary = "获取概览数据")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> getOverview(
            @RequestAttribute("enterpriseId") Long enterpriseId) {
        return Result.success(dashboardService.getOverview(enterpriseId));
    }

    @Operation(summary = "获取趋势数据")
    @GetMapping("/trend")
    public Result<DashboardTrendVO> getTrend(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }

        return Result.success(dashboardService.getTrend(enterpriseId, startDate, endDate));
    }
}
