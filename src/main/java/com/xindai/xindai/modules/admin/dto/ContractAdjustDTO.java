package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同利率调整DTO
 */
@Data
@Schema(description = "合同利率调整参数")
public class ContractAdjustDTO {

    @Schema(description = "新利率", required = true)
    @NotNull(message = "新利率不能为空")
    @DecimalMin(value = "0.0", message = "利率不能为负数")
    private BigDecimal newInterestRate;

    @Schema(description = "调整原因")
    private String reason;
}
