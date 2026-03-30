package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 逾期还款VO（含借款人信息）
 */
@Data
@Schema(description = "逾期还款信息（含借款人）")
public class OverdueRepaymentVO {

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

    @Schema(description = "应还总额")
    private BigDecimal totalAmount;

    @Schema(description = "罚息金额")
    private BigDecimal penaltyAmount;

    @Schema(description = "逾期天数")
    private Integer overdueDays;

    @Schema(description = "合计应还（含罚息）")
    private BigDecimal totalWithPenalty;
}
