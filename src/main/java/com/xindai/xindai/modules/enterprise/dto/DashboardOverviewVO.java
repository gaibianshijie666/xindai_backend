package com.xindai.xindai.modules.enterprise.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据看板概览VO
 */
@Data
public class DashboardOverviewVO {
    // 今日数据
    private Integer todayNewCustomers;
    private Integer todayLoanCount;
    private BigDecimal todayLoanAmount;

    // 累计数据
    private Integer totalCustomers;
    private Integer totalLoanCount;
    private BigDecimal totalLoanAmount;

    // 风险分布
    private RiskDistribution riskDistribution;

    @Data
    public static class RiskDistribution {
        private Integer low;     // 低风险客户数
        private Integer medium;  // 中风险客户数
        private Integer high;    // 高风险客户数
    }
}
