package com.xindai.xindai.modules.enterprise.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日借款统计DTO
 */
@Data
public class DailyLoanStats {
    /**
     * 日期
     */
    private LocalDate date;

    /**
     * 借款数量
     */
    private Integer loanCount;

    /**
     * 借款金额
     */
    private BigDecimal loanAmount;

    /**
     * 通过数量（管理端统计用）
     */
    private Long approvedCount;

    /**
     * 拒绝数量（管理端统计用）
     */
    private Long rejectedCount;
}
