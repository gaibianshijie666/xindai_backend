package com.xindai.xindai.modules.enterprise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("enterprise_user")
public class EnterpriseUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("enterprise_id")
    private Long enterpriseId;

    private String username;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("real_name")
    private String realName;

    private String phone;

    private Integer role;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
