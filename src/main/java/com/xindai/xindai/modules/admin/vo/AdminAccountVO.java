package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端管理员账号VO
 */
@Data
@Schema(description = "管理员账号信息")
public class AdminAccountVO {

    @Schema(description = "管理员ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "角色: ADMIN=普通管理员, SUPER_ADMIN=超级管理员")
    private String role;

    @Schema(description = "状态: 0=禁用, 1=正常")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;
}
