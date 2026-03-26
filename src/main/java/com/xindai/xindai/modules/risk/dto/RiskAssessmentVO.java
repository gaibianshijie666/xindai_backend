package com.xindai.xindai.modules.risk.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class RiskAssessmentVO {
    private Long id;
    private String assessmentNo;
    private Long userId;
    private Long applicationId;
    private Integer assessmentType;
    private BigDecimal riskScore;
    private Integer riskLevel;
    private String decision;
    private String modelVersion;
    private Map<String, Object> factors;
    private Integer processingTimeMs;
    private LocalDateTime createdAt;
}
