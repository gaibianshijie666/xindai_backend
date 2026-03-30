package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端风险评估VO
 */
@Data
@Schema(description = "管理端风险评估信息")
public class AdminRiskAssessmentVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "评估编号")
    private String assessmentNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "申请ID")
    private Long applicationId;

    @Schema(description = "评估类型: 1=额度评估, 2=借款评估, 3=贷后监控")
    private Integer assessmentType;

    @Schema(description = "风险分数")
    private BigDecimal riskScore;

    @Schema(description = "风险等级: 1=低, 2=中, 3=高")
    private Integer riskLevel;

    @Schema(description = "原始决策")
    private String decision;

    @Schema(description = "是否已被覆盖")
    private Boolean overridden;

    @Schema(description = "覆盖决策")
    private String overrideDecision;

    @Schema(description = "模型版本")
    private String modelVersion;

    @Schema(description = "处理时间(毫秒)")
    private Integer processingTimeMs;

    @Schema(description = "置信度")
    private BigDecimal confidence;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "覆盖时间")
    private LocalDateTime overrideAt;

    @Schema(description = "覆盖人ID")
    private Long overrideBy;
}
