package com.xindai.xindai.modules.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.report.vo.UserGrowthVO;
import com.xindai.xindai.modules.report.vo.CreditUsageVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Admin report service interface
 */
public interface AdminReportService {

    /**
     * Get user growth statistics (daily registrations)
     */
    Page<UserGrowthVO> getUserGrowth(Integer page, Integer size, String startDate, String endDate);

    /**
     * Export user growth report as Excel
     */
    void exportUserGrowth(HttpServletResponse response, String startDate, String endDate);

    /**
     * Get credit utilization statistics
     */
    CreditUsageVO getCreditUsage();

    /**
     * Export credit usage report as Excel
     */
    void exportCreditUsage(HttpServletResponse response);
}
