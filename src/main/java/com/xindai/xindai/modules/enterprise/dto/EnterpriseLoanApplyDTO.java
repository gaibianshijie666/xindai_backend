package com.xindai.xindai.modules.enterprise.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class EnterpriseLoanApplyDTO {
    @NotNull(message = "客户ID不能为空")
    private Long enterpriseCustomerId;

    @NotNull(message = "借款金额不能为空")
    @DecimalMin(value = "1000", message = "借款金额不能低于1000元")
    @DecimalMax(value = "500000", message = "借款金额不能超过500000元")
    private BigDecimal amount;

    @NotNull(message = "借款期限不能为空")
    @Min(value = 3, message = "借款期限不能少于3个月")
    @Max(value = 36, message = "借款期限不能超过36个月")
    private Integer term;

    @NotBlank(message = "借款用途不能为空")
    private String purpose;
}
