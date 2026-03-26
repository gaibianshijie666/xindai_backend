package com.xindai.xindai.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "实名认证请求")
public class VerifyIdentityDTO {

    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名", example = "张三", required = true)
    private String realName;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$",
            message = "身份证号格式不正确")
    @Schema(description = "身份证号", example = "110101199001011234", required = true)
    private String idCard;
}
