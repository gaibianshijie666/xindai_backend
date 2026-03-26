package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理端仪表盘概览VO
 */
@Data
@Schema(description = "管理端仪表盘概览数据")
public class DashboardOverviewVO {

    // 今日数据
    @Schema(description = "今日申请数")
    private Integer todayApplications;

    @Schema(description = "今日通过数")
    private Integer todayApproved;

    @Schema(description = "今日拒绝数")
    private Integer todayRejected;

    @Schema(description = "通过率(百分比)")
    private BigDecimal approvalRate;

    // 累计数据
    @Schema(description = "总用户数")
    private Integer totalUsers;

    @Schema(description = "总借款数")
    private Integer totalLoans;

    @Schema(description = "总借款金额")
    private BigDecimal totalAmount;

    @Schema(description = "逾期率(百分比)")
    private BigDecimal overdueRate;

    // 风险分布
    @Schema(description = "风险分布")
    private RiskDistribution riskDistribution;

    @Data
    @Schema(description = "风险分布")
    public static class RiskDistribution {
        @Schema(description = "低风险用户数")
        private Integer low;

        @Schema(description = "中风险用户数")
        private Integer medium;

        @Schema(description = "高风险用户数")
        private Integer high;
    }
}
