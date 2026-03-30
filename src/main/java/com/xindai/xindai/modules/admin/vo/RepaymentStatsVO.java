package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 还款统计VO
 */
@Data
@Schema(description = "还款统计信息")
public class RepaymentStatsVO {

    @Schema(description = "累计已收金额")
    private BigDecimal totalCollected;

    @Schema(description = "待收金额")
    private BigDecimal outstanding;

    @Schema(description = "逾期金额")
    private BigDecimal overdueAmount;

    @Schema(description = "还款率（百分比）")
    private BigDecimal collectionRate;

    @Schema(description = "待还款笔数")
    private Long pendingCount;

    @Schema(description = "已还款笔数")
    private Long paidCount;

    @Schema(description = "逾期笔数")
    private Long overdueCount;
}
