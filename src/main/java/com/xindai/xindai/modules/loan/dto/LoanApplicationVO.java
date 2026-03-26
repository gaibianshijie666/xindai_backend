package com.xindai.xindai.modules.loan.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoanApplicationVO {
    private Long id;
    private String applicationNo;
    private Long userId;
    private BigDecimal amount;
    private Integer term;
    private String purpose;
    private Integer status;
    private String createdAt;
    private String reviewedAt;
}
