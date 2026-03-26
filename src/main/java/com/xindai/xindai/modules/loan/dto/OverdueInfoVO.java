package com.xindai.xindai.modules.loan.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OverdueInfoVO {
    private Long contractId;
    private String contractNo;
    private Integer period;
    private LocalDate dueDate;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal penaltyAmount;
    private Integer overdueDays;
    private BigDecimal totalOverdueAmount;
}
