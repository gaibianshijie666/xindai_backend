package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 管理员角色更新DTO
 */
@Data
@Schema(description = "管理员角色更新参数")
public class AdminRoleDTO {

    @Schema(description = "角色: ADMIN=普通管理员, SUPER_ADMIN=超级管理员", example = "SUPER_ADMIN")
    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(ADMIN|SUPER_ADMIN)$", message = "角色必须为ADMIN或SUPER_ADMIN")
    private String role;
}
