package com.xindai.xindai.modules.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "还款请求")
public class RepayDTO {

    @NotNull(message = "合同ID不能为空")
    @Schema(description = "合同ID", example = "1", required = true)
    private Long contractId;

    @Schema(description = "期数，不填则还全部待还期数", example = "1")
    private Integer period;

    @NotNull(message = "还款金额不能为空")
    @Positive(message = "还款金额必须大于0")
    @Schema(description = "还款金额", example = "1000.00", required = true)
    private BigDecimal amount;
}
