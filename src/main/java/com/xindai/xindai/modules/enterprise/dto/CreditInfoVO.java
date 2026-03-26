package com.xindai.xindai.modules.enterprise.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreditInfoVO {
    private BigDecimal creditLimit;
    private BigDecimal usedLimit;
    private BigDecimal availableLimit;
    private LocalDateTime expireAt;
}
