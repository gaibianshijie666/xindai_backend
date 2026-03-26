package com.xindai.xindai.modules.loan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("loan_application")
public class LoanApplication {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("application_no")
    private String applicationNo;

    @TableField("user_id")
    private Long userId;

    @TableField("enterprise_id")
    private Long enterpriseId;

    @TableField("enterprise_customer_id")
    private Long enterpriseCustomerId;

    private BigDecimal amount;

    private Integer term;

    private String purpose;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("review_note")
    private String reviewNote;
}
