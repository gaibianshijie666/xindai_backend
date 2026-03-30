package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端借款合同详情VO
 */
@Data
@Schema(description = "管理端借款合同详情")
public class AdminContractDetailVO {

    @Schema(description = "合同ID")
    private Long id;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "申请ID")
    private Long applicationId;

    @Schema(description = "借款本金")
    private BigDecimal principal;

    @Schema(description = "年利率")
    private BigDecimal interestRate;

    @Schema(description = "应还总额")
    private BigDecimal totalRepayment;

    @Schema(description = "借款期限(月)")
    private Integer term;

    @Schema(description = "合同状态: 0=待放款, 1=还款中, 2=已结清, 3=逾期")
    private Integer status;

    @Schema(description = "放款时间")
    private LocalDateTime disbursedAt;

    @Schema(description = "到期日期")
    private LocalDate dueDate;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "已还期数")
    private Integer paidPeriods;

    @Schema(description = "待还期数")
    private Integer pendingPeriods;

    @Schema(description = "逾期期数")
    private Integer overduePeriods;

    @Schema(description = "已还金额")
    private BigDecimal paidAmount;

    @Schema(description = "剩余应还金额")
    private BigDecimal remainingAmount;

    @Schema(description = "还款计划列表")
    private List<RepaymentSummaryVO> repaymentPlans;

    /**
     * 还款计划摘要
     */
    @Data
    @Schema(description = "还款计划摘要")
    public static class RepaymentSummaryVO {
        @Schema(description = "期数")
        private Integer period;

        @Schema(description = "应还日期")
        private LocalDate dueDate;

        @Schema(description = "应还本金")
        private BigDecimal principal;

        @Schema(description = "应还利息")
        private BigDecimal interest;

        @Schema(description = "应还总额")
        private BigDecimal totalAmount;

        @Schema(description = "状态: 0=待还款, 1=已还款, 2=已逾期")
        private Integer status;

        @Schema(description = "逾期天数")
        private Integer overdueDays;

        @Schema(description = "罚息金额")
        private BigDecimal penaltyAmount;

        @Schema(description = "实还时间")
        private LocalDateTime paidAt;
    }
}
