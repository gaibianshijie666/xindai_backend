package com.xindai.xindai.modules.report.controller;

import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Operation(summary = "导出借款台账")
    @GetMapping("/loan-ledger")
    public void exportLoanLedger(HttpServletResponse response) {
        reportService.exportLoanLedger(response);
    }

    @Operation(summary = "导出逾期报表")
    @GetMapping("/overdue-report")
    public void exportOverdueReport(HttpServletResponse response) {
        reportService.exportOverdueReport(response);
    }

    @Operation(summary = "导出风控统计报表")
    @GetMapping("/risk-stats")
    public void exportRiskStats(HttpServletResponse response) {
        reportService.exportRiskStats(response);
    }

    @Operation(summary = "导出催收绩效报表")
    @GetMapping("/collection-performance")
    public void exportCollectionPerformance(HttpServletResponse response) {
        reportService.exportCollectionPerformance(response);
    }
}
