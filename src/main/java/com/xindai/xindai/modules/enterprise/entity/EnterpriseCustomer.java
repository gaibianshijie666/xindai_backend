package com.xindai.xindai.modules.enterprise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("enterprise_customer")
public class EnterpriseCustomer {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("enterprise_id")
    private Long enterpriseId;

    @TableField("customer_no")
    private String customerNo;

    @TableField("real_name")
    private String realName;

    @TableField("id_card")
    private String idCard;

    private String phone;

    @TableField("credit_score")
    private Integer creditScore;

    @TableField("risk_level")
    private Integer riskLevel;

    @TableField("total_loan_count")
    private Integer totalLoanCount;

    @TableField("total_loan_amount")
    private BigDecimal totalLoanAmount;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
