package com.xindai.xindai.modules.loan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("disbursement_record")
public class DisbursementRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("contract_id")
    private Long contractId;

    @TableField("application_id")
    private Long applicationId;

    @TableField("user_id")
    private Long userId;

    private BigDecimal amount;

    @TableField("bank_account_id")
    private Long bankAccountId;

    private Integer status;

    @TableField("transaction_no")
    private String transactionNo;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("failed_reason")
    private String failedReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
