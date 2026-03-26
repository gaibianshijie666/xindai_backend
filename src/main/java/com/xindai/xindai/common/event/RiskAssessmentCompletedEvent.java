package com.xindai.xindai.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class RiskAssessmentCompletedEvent extends DomainEvent {
    private Long applicationId;
    private Long userId;
    private String riskLevel;
    private BigDecimal creditLimit;
    private Integer creditGrade;
}
