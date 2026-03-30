package com.xindai.xindai.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "修改密码请求")
public class PasswordChangeDTO {

    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "旧密码", required = true)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d!@#$%^&*()_+\\-=]{8,20}$",
            message = "密码需8-20位，包含大小写字母和数字")
    @Schema(description = "新密码", required = true)
    private String newPassword;
}
