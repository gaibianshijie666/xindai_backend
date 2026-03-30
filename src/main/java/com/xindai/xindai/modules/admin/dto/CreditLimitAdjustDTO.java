package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 授信额度调整DTO
 */
@Data
@Schema(description = "授信额度调整参数")
public class CreditLimitAdjustDTO {

    @Schema(description = "授信额度", example = "500000.00")
    @NotNull(message = "授信额度不能为空")
    @DecimalMin(value = "0.01", message = "授信额度必须大于0")
    private BigDecimal creditLimit;

    @Schema(description = "调整原因", example = "企业经营状况良好，提升授信额度")
    private String reason;
}
