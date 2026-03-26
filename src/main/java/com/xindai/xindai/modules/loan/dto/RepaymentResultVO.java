package com.xindai.xindai.modules.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "还款结果")
public class RepaymentResultVO {

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "消息")
    private String message;

    @Schema(description = "还款金额")
    private BigDecimal repaidAmount;

    @Schema(description = "已还期数")
    private List<RepaidPeriod> repaidPeriods;

    @Schema(description = "合同剩余待还金额")
    private BigDecimal remainingAmount;

    @Schema(description = "合同是否已结清")
    private Boolean contractSettled;

    @Data
    @Schema(description = "已还期数详情")
    public static class RepaidPeriod {
        @Schema(description = "期数")
        private Integer period;

        @Schema(description = "还款金额")
        private BigDecimal amount;

        @Schema(description = "还款时间")
        private String paidAt;
    }
}
