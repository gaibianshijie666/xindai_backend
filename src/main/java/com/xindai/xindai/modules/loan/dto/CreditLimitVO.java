package com.xindai.xindai.modules.loan.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreditLimitVO {
    private Long id;
    private Long userId;
    private BigDecimal totalLimit;
    private BigDecimal usedLimit;
    private BigDecimal availableLimit;
    private Integer status;
    private String expireAt;

    // 额度计算详情
    private BigDecimal baseLimit;           // 基础额度（基于收入）
    private Double incomeMultiplier;        // 收入倍数
    private Double gradeFactor;             // 信用等级系数
    private Double employmentFactor;        // 就业年限系数
    private Double dtiFactor;               // DTI调整系数
    private Double riskAdjustFactor;        // 风险调整系数
    private String explanation;             // 额度说明
}
