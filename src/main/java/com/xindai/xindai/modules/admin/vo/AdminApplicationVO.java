package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端借款申请VO
 */
@Data
@Schema(description = "管理端借款申请信息")
public class AdminApplicationVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "申请编号")
    private String applicationNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "申请金额")
    private BigDecimal amount;

    @Schema(description = "借款期限(月)")
    private Integer term;

    @Schema(description = "借款用途")
    private String purpose;

    @Schema(description = "申请状态: 0=待审核, 1=审核中, 2=已通过, 3=已拒绝")
    private Integer status;

    @Schema(description = "风险评分")
    private BigDecimal riskScore;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核人姓名")
    private String reviewerName;

    @Schema(description = "审核备注")
    private String reviewNote;
}
