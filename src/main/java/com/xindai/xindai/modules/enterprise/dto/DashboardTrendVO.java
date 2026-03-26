package com.xindai.xindai.modules.enterprise.dto;

import lombok.Data;

import java.util.List;

/**
 * 数据看板趋势VO
 */
@Data
public class DashboardTrendVO {
    private List<TrendItem> items;

    @Data
    public static class TrendItem {
        private String date;
        private Integer loanCount;
        private java.math.BigDecimal loanAmount;
        private Integer newCustomerCount;
    }
}
