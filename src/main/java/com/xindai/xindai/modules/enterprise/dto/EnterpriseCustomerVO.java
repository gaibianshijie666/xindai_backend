package com.xindai.xindai.modules.enterprise.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EnterpriseCustomerVO {
    private Long id;
    private String customerNo;
    private String realName;
    private String idCard;
    private String phone;
    private Integer creditScore;
    private Integer riskLevel;
    private Integer totalLoanCount;
    private BigDecimal totalLoanAmount;
    private Integer status;
    private LocalDateTime createdAt;
}
