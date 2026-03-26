package com.xindai.xindai.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class LoanApplicationApprovedEvent extends DomainEvent {
    private Long applicationId;
    private Long userId;
    private BigDecimal amount;
    private BigDecimal approvedAmount;
    private String reviewNote;
}
