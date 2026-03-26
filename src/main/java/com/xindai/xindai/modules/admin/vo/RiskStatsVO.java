package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 风控统计VO
 */
@Data
@Schema(description = "风控统计数据")
public class RiskStatsVO {

    @Schema(description = "统计范围: 7d/30d/90d")
    private String range;

    @Schema(description = "风险等级分布")
    private RiskDistribution distribution;

    @Schema(description = "每日统计数据")
    private List<DailyStats> dailyStats;

    @Data
    @Schema(description = "风险等级分布")
    public static class RiskDistribution {
        @Schema(description = "低风险占比(百分比)")
        private BigDecimal lowPercent;

        @Schema(description = "中风险占比(百分比)")
        private BigDecimal mediumPercent;

        @Schema(description = "高风险占比(百分比)")
        private BigDecimal highPercent;
    }

    @Data
    @Schema(description = "每日统计")
    public static class DailyStats {
        @Schema(description = "日期")
        private String date;

        @Schema(description = "申请数")
        private Integer applications;

        @Schema(description = "通过数")
        private Integer approved;

        @Schema(description = "拒绝数")
        private Integer rejected;

        @Schema(description = "平均风险评分")
        private BigDecimal avgRiskScore;
    }
}
