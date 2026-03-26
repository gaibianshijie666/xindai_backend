package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 用户状态更新DTO
 */
@Data
@Schema(description = "用户状态更新参数")
public class UserStatusUpdateDTO {

    @Schema(description = "用户状态: 0=禁用, 1=正常", example = "1")
    @Min(value = 0, message = "状态值必须为0或1")
    @Max(value = 1, message = "状态值必须为0或1")
    private Integer status;
}
