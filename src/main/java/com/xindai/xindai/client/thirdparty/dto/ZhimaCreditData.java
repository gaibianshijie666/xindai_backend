package com.xindai.xindai.client.thirdparty.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ZhimaCreditData {
    // 芝麻分
    private Integer zhimaScore;
    private String creditLevel;

    // 身份核验
    private Boolean idVerified;
    private Boolean phoneVerified;
    private Boolean bankCardVerified;

    // 行业关注名单
    private Boolean inIndustryWatchlist;
    private String watchlistReason;

    // 欺诈风险
    private Boolean fraudRisk;
    private BigDecimal fraudScore;
    private String fraudLevel;

    // 信用行为
    private BigDecimal behaviorScore;
    private BigDecimal repaymentWillScore;
    private BigDecimal repaymentAbilityScore;

    // 稳定性评估
    private BigDecimal stabilityScore;
    private BigDecimal socialScore;
}
