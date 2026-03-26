package com.xindai.xindai.client.thirdparty.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreditReportData {
    // 基础信息
    private String reportNo;
    private String name;
    private String idCard;

    // 信用评分
    private Integer creditScore;
    private String creditLevel;

    // 负债信息
    private BigDecimal totalDebt;
    private BigDecimal monthlyPayment;
    private BigDecimal debtRatio;
    private Integer accountCount;

    // 逾期信息
    private Integer overdueCount;
    private Integer overdueCount90d;
    private Integer overdueCount180d;
    private BigDecimal maxOverdueAmount;

    // 查询记录
    private Integer inquiryCount1m;
    private Integer inquiryCount3m;
    private Integer inquiryCount6m;

    // 账户信息
    private List<CreditAccount> accounts;

    @Data
    public static class CreditAccount {
        private String accountType;
        private String bankName;
        private BigDecimal creditLimit;
        private BigDecimal balance;
        private String status;
        private Integer overdueDays;
    }
}
