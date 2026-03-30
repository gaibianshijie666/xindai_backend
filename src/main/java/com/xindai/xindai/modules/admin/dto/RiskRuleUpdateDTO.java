package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 风险规则更新DTO (保留接口,当前只读)
 */
@Data
@Schema(description = "风险规则更新参数 (保留接口,当前只读)")
public class RiskRuleUpdateDTO {

    @Schema(description = "自动通过阈值 (分数低于此值自动通过)")
    private Integer autoApproveThreshold;

    @Schema(description = "自动拒绝阈值 (分数高于此值自动拒绝)")
    private Integer autoRejectThreshold;

    @Schema(description = "人工审核最小分数")
    private Integer manualReviewMinScore;

    @Schema(description = "人工审核最大分数")
    private Integer manualReviewMaxScore;
}
