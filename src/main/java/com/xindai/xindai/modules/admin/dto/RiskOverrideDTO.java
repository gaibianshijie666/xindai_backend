package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 风险决策覆盖DTO
 */
@Data
@Schema(description = "风险决策覆盖参数")
public class RiskOverrideDTO {

    @Schema(description = "新决策: APPROVE, REJECT", required = true)
    @NotBlank(message = "新决策不能为空")
    @Pattern(regexp = "APPROVE|REJECT", message = "决策必须是APPROVE或REJECT")
    private String newDecision;

    @Schema(description = "覆盖原因", required = true)
    @NotBlank(message = "覆盖原因不能为空")
    private String reason;
}
