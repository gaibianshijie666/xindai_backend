package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端还款计划VO
 */
@Data
@Schema(description = "管理端还款计划信息")
public class AdminRepaymentVO {

    @Schema(description = "还款计划ID")
    private Long id;

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "用户手机号")
    private String userPhone;

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

    @Schema(description = "还款状态: 0=待还款, 1=已还款, 2=已逾期")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "罚息金额")
    private BigDecimal penaltyAmount;

    @Schema(description = "逾期天数")
    private Integer overdueDays;

    @Schema(description = "实际还款时间")
    private LocalDateTime paidAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
