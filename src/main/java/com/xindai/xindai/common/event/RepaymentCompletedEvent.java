package com.xindai.xindai.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class RepaymentCompletedEvent extends DomainEvent {
    private Long contractId;
    private Long userId;
    private Integer period;
    private BigDecimal amount;
}
