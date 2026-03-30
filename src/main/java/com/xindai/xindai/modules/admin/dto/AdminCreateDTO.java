package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员创建DTO
 */
@Data
@Schema(description = "管理员创建参数")
public class AdminCreateDTO {

    @Schema(description = "用户名", example = "admin01")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度为3-20个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @Schema(description = "密码", example = "Admin@123")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20个字符")
    private String password;

    @Schema(description = "真实姓名", example = "张三")
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 20, message = "真实姓名不能超过20个字符")
    private String realName;

    @Schema(description = "手机号", example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "角色: ADMIN=普通管理员, SUPER_ADMIN=超级管理员", example = "ADMIN")
    @NotBlank(message = "角色不能为空")
    private String role;
}
