package com.xindai.xindai.modules.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanApplicationDTO {
    @NotNull(message = "借款金额不能为空")
    @DecimalMin(value = "1000", message = "借款金额最低1000元")
    private BigDecimal amount;

    @NotNull(message = "借款期限不能为空")
    @Min(value = 3, message = "借款期限最少3个月")
    private Integer term;

    private String purpose;
}
