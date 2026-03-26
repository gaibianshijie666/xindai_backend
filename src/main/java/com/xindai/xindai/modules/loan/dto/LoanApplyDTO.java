package com.xindai.xindai.modules.loan.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanApplyDTO {
    @NotNull(message = "借款金额不能为空")
    @DecimalMin(value = "1000", message = "最小借款金额为1000元")
    @DecimalMax(value = "500000", message = "最大借款金额为50万元")
    private BigDecimal amount;

    @NotNull(message = "借款期限不能为空")
    @Min(value = 3, message = "最小借款期限为3个月")
    @Max(value = 36, message = "最大借款期限为36个月")
    private Integer term;

    private String purpose;
}
