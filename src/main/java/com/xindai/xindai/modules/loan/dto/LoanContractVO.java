package com.xindai.xindai.modules.loan.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanContractVO {
    private Long id;
    private String contractNo;
    private Long applicationId;
    private BigDecimal principal;
    private BigDecimal interestRate;
    private BigDecimal totalRepayment;
    private Integer term;
    private Integer status;
    private String disbursedAt;
    private LocalDate dueDate;
    private String createdAt;
}
