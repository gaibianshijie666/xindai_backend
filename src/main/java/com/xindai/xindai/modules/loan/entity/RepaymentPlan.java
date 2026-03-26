package com.xindai.xindai.modules.loan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("repayment_plan")
public class RepaymentPlan {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("contract_id")
    private Long contractId;

    private Integer period;

    @TableField("due_date")
    private LocalDate dueDate;

    private BigDecimal principal;

    private BigDecimal interest;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    // 状态: 0=待还款(PENDING), 1=已还款(PAID), 2=已逾期(OVERDUE)，见RepaymentStatus枚举
    private Integer status;

    @TableField("penalty_amount")
    private BigDecimal penaltyAmount;

    @TableField("overdue_days")
    private Integer overdueDays;

    @TableField("paid_at")
    private LocalDateTime paidAt;
}
