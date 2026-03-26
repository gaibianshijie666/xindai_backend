package com.xindai.xindai.modules.report.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 报表导出服务接口
 */
public interface ReportService {

    /**
     * 导出借款台账
     */
    void exportLoanLedger(HttpServletResponse response);

    /**
     * 导出逾期报表
     */
    void exportOverdueReport(HttpServletResponse response);

    /**
     * 导出风控统计报表
     */
    void exportRiskStats(HttpServletResponse response);

    /**
     * 导出催收绩效报表
     */
    void exportCollectionPerformance(HttpServletResponse response);
}
