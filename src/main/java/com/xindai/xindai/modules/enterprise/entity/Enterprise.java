package com.xindai.xindai.modules.enterprise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ToString(exclude = {"apiKey", "unifiedSocialCreditCode"})
@TableName("enterprise")
public class Enterprise {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("enterprise_no")
    private String enterpriseNo;

    private String name;

    @TableField("unified_social_credit_code")
    private String unifiedSocialCreditCode;

    @TableField("legal_person")
    private String legalPerson;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("enterprise_type")
    private Integer enterpriseType;

    private Integer status;

    @TableField("credit_limit")
    private BigDecimal creditLimit;

    @TableField("used_limit")
    private BigDecimal usedLimit;

    @TableField("api_key")
    private String apiKey;

    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
