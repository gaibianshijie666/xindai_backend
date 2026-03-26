package com.xindai.xindai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "credit-limit")
public class CreditLimitProperties {

    private BigDecimal defaultLimit = new BigDecimal("10000");
    private BigDecimal minLimit = new BigDecimal("1000");
    private BigDecimal maxLimit = new BigDecimal("500000");

    private double incomeMultiplierLow = 0.3;
    private double incomeMultiplierMedium = 0.5;
    private double incomeMultiplierHigh = 0.6;

    private Map<String, Double> gradeMultipliers = new HashMap<>(Map.of(
        "A", 1.5, "B", 1.3, "C", 1.1, "D", 1.0,
        "E", 0.8, "F", 0.6, "G", 0.4
    ));

    private Map<String, Double> employmentYearMultipliers = new HashMap<>(Map.of(
        "less-than-1", 0.8, "1-to-3", 0.9, "3-to-5", 1.0,
        "5-to-10", 1.1, "over-10", 1.2
    ));

    private Map<String, Double> dtiAdjustments = new HashMap<>(Map.of(
        "low", 1.0, "medium", 0.9, "high", 0.7, "very-high", 0.5
    ));

    /**
     * 根据信用等级配置年化利率（单位：%）
     * key: 信用等级（A-G），value: 年化利率
     */
    private Map<String, BigDecimal> interestRatesByGrade = new HashMap<>(Map.of(
        "A", new BigDecimal("8"),
        "B", new BigDecimal("11"),
        "C", new BigDecimal("14"),
        "D", new BigDecimal("18"),
        "E", new BigDecimal("22"),
        "F", new BigDecimal("26"),
        "G", new BigDecimal("30")
    ));

    /**
     * 默认利率（当信用等级不在配置范围内时使用）
     */
    private BigDecimal defaultInterestRate = new BigDecimal("15");

    /**
     * 根据信用等级获取利率
     */
    public BigDecimal getInterestRate(String grade) {
        if (grade == null) {
            return defaultInterestRate;
        }
        BigDecimal rate = interestRatesByGrade.get(grade.toUpperCase());
        return rate != null ? rate : defaultInterestRate;
    }
}
