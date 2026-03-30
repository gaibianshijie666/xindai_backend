package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 风险规则配置VO
 */
@Data
@Schema(description = "风险规则配置信息")
public class RiskRuleVO {

    @Schema(description = "自动通过阈值 (分数低于此值自动通过)")
    private Integer autoApproveThreshold;

    @Schema(description = "自动拒绝阈值 (分数高于此值自动拒绝)")
    private Integer autoRejectThreshold;

    @Schema(description = "人工审核分数范围")
    private ManualReviewRange manualReviewRange;

    @Schema(description = "模型版本")
    private String modelVersion;

    @Schema(description = "评分权重配置")
    private Map<String, Double> scoringWeights;

    @Schema(description = "置信度阈值")
    private Double confidenceThreshold;

    @Data
    @Schema(description = "人工审核分数范围")
    public static class ManualReviewRange {
        @Schema(description = "最小分数")
        private Integer minScore;

        @Schema(description = "最大分数")
        private Integer maxScore;
    }
}
