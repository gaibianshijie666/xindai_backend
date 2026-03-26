package com.xindai.xindai.modules.loan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bank_account")
public class BankAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("bank_name")
    private String bankName;

    @TableField("account_no")
    private String accountNo;

    @TableField("account_name")
    private String accountName;

    @TableField("is_default")
    private Integer isDefault;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
