package com.xindai.xindai.modules.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "额度预估结果")
public class LimitEstimateVO {

    @Schema(description = "预估可贷额度")
    private BigDecimal estimatedLimit;

    @Schema(description = "信用评分（300-850）")
    private Integer creditScore;

    @Schema(description = "风险等级：LOW(低)、MEDIUM(中)、HIGH(高)")
    private String riskLevel;

    @Schema(description = "建议借款期限（月）")
    private Integer suggestedTerm;

    @Schema(description = "参考年化利率")
    private BigDecimal referenceRate;

    @Schema(description = "评估因素")
    private List<EstimateFactor> factors;

    @Data
    @Schema(description = "评估因素")
    public static class EstimateFactor {
        @Schema(description = "因素名称")
        private String name;

        @Schema(description = "因素描述")
        private String description;

        @Schema(description = "影响程度：POSITIVE(正面)、NEUTRAL(中性)、NEGATIVE(负面)")
        private String impact;

        @Schema(description = "权重")
        private BigDecimal weight;
    }
}
