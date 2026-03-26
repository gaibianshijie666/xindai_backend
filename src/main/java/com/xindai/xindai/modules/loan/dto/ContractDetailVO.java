package com.xindai.xindai.modules.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "合同详情")
public class ContractDetailVO {

    @Schema(description = "合同ID")
    private Long id;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "申请ID")
    private Long applicationId;

    @Schema(description = "本金")
    private BigDecimal principal;

    @Schema(description = "年化利率")
    private BigDecimal interestRate;

    @Schema(description = "总还款金额")
    private BigDecimal totalRepayment;

    @Schema(description = "已还金额")
    private BigDecimal repaidAmount;

    @Schema(description = "待还金额")
    private BigDecimal pendingAmount;

    @Schema(description = "借款期限（月）")
    private Integer term;

    @Schema(description = "已还期数")
    private Integer repaidPeriods;

    @Schema(description = "待还期数")
    private Integer pendingPeriods;

    @Schema(description = "状态：0-待放款、1-还款中、2-已结清、3-已逾期")
    private Integer status;

    @Schema(description = "放款时间")
    private String disbursedAt;

    @Schema(description = "到期日")
    private LocalDate dueDate;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "还款计划列表")
    private List<RepaymentPlanVO> repaymentPlans;
}
