package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端放款记录VO
 */
@Data
@Schema(description = "管理端放款记录信息")
public class AdminDisbursementVO {

    @Schema(description = "放款记录ID")
    private Long id;

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "申请ID")
    private Long applicationId;

    @Schema(description = "放款金额")
    private BigDecimal amount;

    @Schema(description = "银行账户ID")
    private Long bankAccountId;

    @Schema(description = "放款状态: 0=待放款, 1=放款中, 2=已放款, 3=放款失败")
    private Integer status;

    @Schema(description = "交易流水号")
    private String transactionNo;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "失败原因")
    private String failedReason;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
