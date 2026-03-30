package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置密码DTO
 */
@Data
@Schema(description = "管理员重置密码参数")
public class AdminResetPasswordDTO {

    @Schema(description = "新密码", example = "NewPass@123")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20个字符")
    private String newPassword;
}
