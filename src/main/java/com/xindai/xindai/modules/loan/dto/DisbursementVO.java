package com.xindai.xindai.modules.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "放款记录视图对象")
public class DisbursementVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "申请ID")
    private Long applicationId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "放款金额")
    private BigDecimal amount;

    @Schema(description = "银行卡ID")
    private Long bankAccountId;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "银行卡号（脱敏）")
    private String accountNo;

    @Schema(description = "状态：0=待放款,1=放款中,2=已放款,3=放款失败")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "交易流水号")
    private String transactionNo;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "失败原因")
    private String failedReason;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
