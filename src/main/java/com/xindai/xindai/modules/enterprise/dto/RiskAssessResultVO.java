package com.xindai.xindai.modules.enterprise.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风险评估结果VO
 */
@Data
public class RiskAssessResultVO {
    private Long customerId;
    private String customerName;
    private Integer riskScore;
    private Integer riskLevel; // 0-低 1-中 2-高
    private String riskAdvice;
    private List<RiskFactor> riskFactors;
    private LocalDateTime assessTime;

    @Data
    public static class RiskFactor {
        private String name;
        private String value;
        private Integer weight;
    }
}
