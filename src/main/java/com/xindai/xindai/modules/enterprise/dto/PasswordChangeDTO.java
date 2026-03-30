package com.xindai.xindai.modules.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "修改密码请求")
@Data
public class PasswordChangeDTO {

    @NotBlank(message = "旧密码不能为空")
    @Size(min = 6, max = 20, message = "旧密码长度必须在6-20位之间")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d!@#$%^&*()_+\\-=]{8,20}$",
            message = "密码需8-20位，包含大小写字母和数字")
    private String newPassword;
}
