package com.xindai.xindai.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户信息更新请求")
public class UserUpdateDTO {

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "身份证号", example = "110101199001011234")
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
