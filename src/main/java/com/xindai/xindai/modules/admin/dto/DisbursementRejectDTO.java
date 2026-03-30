package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 放款拒绝DTO
 */
@Data
@Schema(description = "放款拒绝参数")
public class DisbursementRejectDTO {

    @Schema(description = "拒绝原因", required = true)
    @NotBlank(message = "拒绝原因不能为空")
    private String reason;
}
