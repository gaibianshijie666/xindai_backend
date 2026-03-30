package com.xindai.xindai.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户信息更新请求")
public class UserUpdateDTO {

    // 实名信息必须通过 verifyIdentity() 方法进行KYC验证，不允许直接更新
    @JsonIgnore
    private String realName;

    @JsonIgnore
    private String idCard;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "工作单位", example = "某某科技有限公司")
    private String company;

    @Schema(description = "职位", example = "软件工程师")
    private String position;

    @Schema(description = "月收入", example = "15000.00")
    private java.math.BigDecimal monthlyIncome;
}
