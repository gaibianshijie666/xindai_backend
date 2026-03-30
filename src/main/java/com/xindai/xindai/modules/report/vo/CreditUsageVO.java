package com.xindai.xindai.modules.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Credit utilization statistics view object
 */
@Data
public class CreditUsageVO {
    private BigDecimal totalLimit;
    private BigDecimal usedLimit;
    private BigDecimal availableLimit;
    private BigDecimal utilizationRate;
    private Long activeUsers;
}
