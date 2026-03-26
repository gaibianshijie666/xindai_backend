package com.xindai.xindai.modules.loan.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RepaymentPlanVO {
    private Long id;
    private Long contractId;
    private String contractNo;
    private Integer period;
    private LocalDate dueDate;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal totalAmount;
    private Integer status;
    private String paidAt;
}
