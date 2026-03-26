package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 黑名单添加DTO
 */
@Data
@Schema(description = "黑名单添加参数")
public class BlacklistAddDTO {

    @Schema(description = "类型: 1=手机号, 2=身份证, 3=设备ID", required = true, example = "1")
    @NotNull(message = "类型不能为空")
    @Min(value = 1, message = "类型必须为1-3")
    @Max(value = 3, message = "类型必须为1-3")
    private Integer type;

    @Schema(description = "值", required = true)
    @NotBlank(message = "值不能为空")
    private String value;

    @Schema(description = "原因", required = true)
    @NotBlank(message = "原因不能为空")
    private String reason;
}
