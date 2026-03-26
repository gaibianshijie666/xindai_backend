package com.xindai.xindai.modules.admin.vo;

import com.xindai.xindai.common.annotation.Desensitize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户列表VO
 */
@Data
@Schema(description = "管理端用户信息")
public class AdminUserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "手机号")
    @Desensitize(Desensitize.DesensitizeType.PHONE)
    private String phone;

    @Schema(description = "真实姓名")
    @Desensitize(Desensitize.DesensitizeType.NAME)
    private String realName;

    @Schema(description = "用户状态: 0=禁用, 1=正常")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
