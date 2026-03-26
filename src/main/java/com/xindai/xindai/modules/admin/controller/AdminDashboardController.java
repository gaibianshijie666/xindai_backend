package com.xindai.xindai.modules.admin.controller;

import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.service.AdminDashboardService;
import com.xindai.xindai.modules.admin.vo.DashboardOverviewVO;
import com.xindai.xindai.modules.admin.vo.RiskStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端仪表盘控制器
 */
@Tag(name = "管理端-数据看板", description = "管理端数据统计接口")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "获取仪表盘概览数据")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> getOverview() {
        return Result.success(adminDashboardService.getOverview());
    }

    @Operation(summary = "获取风控统计数据")
    @GetMapping("/risk-stats")
    public Result<RiskStatsVO> getRiskStats(
            @Parameter(description = "统计范围: 7d/30d/90d") @RequestParam(required = false) String range) {
        return Result.success(adminDashboardService.getRiskStats(range));
    }
}
