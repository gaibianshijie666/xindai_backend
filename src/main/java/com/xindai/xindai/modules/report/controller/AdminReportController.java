package com.xindai.xindai.modules.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.report.service.AdminReportService;
import com.xindai.xindai.modules.report.service.ReportService;
import com.xindai.xindai.modules.report.vo.CreditUsageVO;
import com.xindai.xindai.modules.report.vo.UserGrowthVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端报表导出控制器
 */
@Tag(name = "管理端-报表导出", description = "管理端报表导出接口")
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReportService reportService;
    private final AdminReportService adminReportService;

    @Operation(summary = "导出借款台账")
    @GetMapping("/loan-ledger")
    public void exportLoanLedger(
            HttpServletResponse response,
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        reportService.exportLoanLedger(response, startDate, endDate);
    }

    @Operation(summary = "导出逾期报表")
    @GetMapping("/overdue-report")
    public void exportOverdueReport(
            HttpServletResponse response,
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        reportService.exportOverdueReport(response, startDate, endDate);
    }

    @Operation(summary = "导出风控统计报表")
    @GetMapping("/risk-stats")
    public void exportRiskStats(
            HttpServletResponse response,
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        reportService.exportRiskStats(response, startDate, endDate);
    }

    @Operation(summary = "导出催收绩效报表")
    @GetMapping("/collection-performance")
    public void exportCollectionPerformance(
            HttpServletResponse response,
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        reportService.exportCollectionPerformance(response, startDate, endDate);
    }

    @Operation(summary = "用户增长统计")
    @GetMapping("/user-growth")
    public Result<Page<UserGrowthVO>> getUserGrowth(
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(required = false, defaultValue = "10") Integer size,
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        return Result.success(adminReportService.getUserGrowth(page, size, startDate, endDate));
    }

    @Operation(summary = "导出用户增长统计")
    @GetMapping("/user-growth/export")
    public void exportUserGrowth(
            HttpServletResponse response,
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        adminReportService.exportUserGrowth(response, startDate, endDate);
    }

    @Operation(summary = "额度使用统计")
    @GetMapping("/credit-usage")
    public Result<CreditUsageVO> getCreditUsage() {
        return Result.success(adminReportService.getCreditUsage());
    }

    @Operation(summary = "导出额度使用统计")
    @GetMapping("/credit-usage/export")
    public void exportCreditUsage(HttpServletResponse response) {
        adminReportService.exportCreditUsage(response);
    }
}
