package com.xindai.xindai.modules.enterprise.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EnterpriseLoanVO {
    private Long id;
    private String applicationNo;
    private Long enterpriseCustomerId;
    private String customerName;
    private String customerIdCard;
    private BigDecimal amount;
    private Integer term;
    private String purpose;
    private Integer status;
    private Integer riskScore;
    private String rejectReason;
    private LocalDateTime createdAt;
}
