package com.xindai.xindai.modules.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "提额申请请求")
public class LimitApplyDTO {

    @NotNull(message = "申请额度不能为空")
    @Positive(message = "申请额度必须大于0")
    @Schema(description = "申请提升的额度", example = "10000.00", required = true)
    private BigDecimal requestedLimit;

    @Schema(description = "申请原因", example = "收入增加")
    private String reason;
}
