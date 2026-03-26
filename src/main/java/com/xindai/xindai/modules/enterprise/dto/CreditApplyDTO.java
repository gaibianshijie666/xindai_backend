package com.xindai.xindai.modules.enterprise.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreditApplyDTO {
    @NotNull
    @DecimalMin("100000")
    @DecimalMax("10000000")
    private BigDecimal requestedLimit;

    private String reason;
}
