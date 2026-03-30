package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 企业状态更新DTO
 */
@Data
@Schema(description = "企业状态更新参数")
public class EnterpriseStatusDTO {

    @Schema(description = "企业状态: 0=待审核, 1=正常, 2=已暂停, 3=已禁用", example = "1")
    @Min(value = 0, message = "状态值必须为0-3")
    @Max(value = 3, message = "状态值必须为0-3")
    private Integer status;
}
