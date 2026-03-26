package com.xindai.xindai.modules.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xindai.xindai.common.mybatis.CryptoField;
import lombok.Data;
import lombok.ToString;
import java.time.LocalDateTime;

@Data
@ToString(exclude = {"passwordHash", "idCard"})
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    @CryptoField
    private String phone;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("real_name")
    private String realName;

    @CryptoField
    @TableField("id_card")
    private String idCard;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
