package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 还款调整DTO（罚息调整）
 */
@Data
@Schema(description = "还款罚息调整参数")
public class RepaymentAdjustDTO {

    @Schema(description = "调整后罚息金额", required = true)
    @NotNull(message = "罚息金额不能为空")
    private BigDecimal penaltyAmount;

    @Schema(description = "调整原因", required = true)
    @NotNull(message = "调整原因不能为空")
    private String reason;
}
