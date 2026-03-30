package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端借款合同VO
 */
@Data
@Schema(description = "管理端借款合同信息")
public class AdminContractVO {

    @Schema(description = "合同ID")
    private Long id;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

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

    @Schema(description = "总期数")
    private Integer totalPeriods;

    @Schema(description = "剩余应还金额")
    private BigDecimal remainingAmount;
}
