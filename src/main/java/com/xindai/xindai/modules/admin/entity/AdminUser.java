package com.xindai.xindai.modules.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("admin_user")
public class AdminUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("real_name")
    private String realName;

    private String phone;

    private String role;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
}
