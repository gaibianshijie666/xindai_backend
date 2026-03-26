package com.xindai.xindai.modules.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "额度预估请求")
public class LimitEstimateDTO {

    @NotNull(message = "月收入不能为空")
    @Positive(message = "月收入必须大于0")
    @Schema(description = "月收入", example = "15000.00", required = true)
    private BigDecimal monthlyIncome;

    @NotNull(message = "工作年限不能为空")
    @Min(value = 0, message = "工作年限不能小于0")
    @Max(value = 50, message = "工作年限不能超过50")
    @Schema(description = "工作年限（年）", example = "5", required = true)
    private Integer workYears;

    @NotNull(message = "学历不能为空")
    @Schema(description = "学历：HIGH_SCHOOL(高中)、COLLEGE(大专)、BACHELOR(本科)、MASTER(硕士)、DOCTOR(博士)", example = "BACHELOR", required = true)
    private String education;

    @Schema(description = "是否有房", example = "true")
    private Boolean hasHouse;

    @Schema(description = "是否有车", example = "false")
    private Boolean hasCar;

    @Schema(description = "现有负债", example = "50000.00")
    private BigDecimal existingDebt;
}
