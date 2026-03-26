package com.xindai.xindai.modules.loan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("loan_contract")
public class LoanContract {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("contract_no")
    private String contractNo;

    @TableField("application_id")
    private Long applicationId;

    @TableField("user_id")
    private Long userId;

    private BigDecimal principal;

    @TableField("interest_rate")
    private BigDecimal interestRate;

    @TableField("total_repayment")
    private BigDecimal totalRepayment;

    private Integer term;

    private Integer status;

    @TableField("disbursed_at")
    private LocalDateTime disbursedAt;

    @TableField("due_date")
    private LocalDate dueDate;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
