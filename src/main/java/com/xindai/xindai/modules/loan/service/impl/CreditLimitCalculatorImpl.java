package com.xindai.xindai.modules.loan.service.impl;

import com.xindai.xindai.config.CreditLimitProperties;
import com.xindai.xindai.modules.loan.entity.CreditLimit;
import com.xindai.xindai.modules.loan.service.CreditLimitCalculator;
import com.xindai.xindai.modules.user.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 智能信用额度计算器实现
 *
 * 额度计算公式：
 * 最终额度 = 基础额度 × 信用等级系数 × 就业年限系数 × DTI调整系数 × 风险系数
 *
 * 其中：
 * - 基础额度 = 年收入 × 收入倍数（0.3-0.8）
 * - 信用等级系数 = 根据creditGrade从配置获取（0.4-1.5）
 * - 就业年限系数 = 根据employmentYears从配置获取（0.8-1.2）
 * - DTI调整系数 = 根据DTI从配置获取（0.5-1.0）
 * - 风险系数 = 根据风险评分（0.5-1.5）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditLimitCalculatorImpl implements CreditLimitCalculator {

    private final CreditLimitProperties properties;

    @Override
    public CreditLimit calculate(UserProfile userProfile, double riskScore, String creditGrade) {
        CreditLimitDetail detail = getCalculationDetail(userProfile, riskScore, creditGrade);

        CreditLimit limit = new CreditLimit();
        limit.setUserId(userProfile != null ? userProfile.getUserId() : null);
        limit.setTotalLimit(detail.finalLimit());
        limit.setUsedLimit(BigDecimal.ZERO);
        limit.setAvailableLimit(detail.finalLimit());
        limit.setStatus(1);
        limit.setExpireAt(LocalDateTime.now().plus(1, ChronoUnit.YEARS));

        log.info("Credit limit calculated: userId={}, baseLimit={}, gradeFactor={}, employmentFactor={}, " +
                "dtiFactor={}, riskFactor={}, finalLimit={}",
                userProfile != null ? userProfile.getUserId() : null, detail.baseLimit(), detail.gradeFactor(),
                detail.employmentFactor(), detail.dtiFactor(), detail.riskAdjustFactor(), detail.finalLimit());

        return limit;
    }

    @Override
    public BigDecimal adjustByRiskScore(BigDecimal currentLimit, double riskScore) {
        double riskFactor = calculateRiskFactor(riskScore);
        BigDecimal adjusted = currentLimit.multiply(BigDecimal.valueOf(riskFactor))
                .setScale(0, RoundingMode.DOWN);

        if (adjusted.compareTo(properties.getMinLimit()) < 0) {
            adjusted = properties.getMinLimit();
        } else if (adjusted.compareTo(properties.getMaxLimit()) > 0) {
            adjusted = properties.getMaxLimit();
        }

        return adjusted;
    }

    @Override
    public CreditLimitDetail getCalculationDetail(UserProfile userProfile, double riskScore, String creditGrade) {
        // 1. 计算基础额度（基于年收入）
        BigDecimal annualIncome = getAnnualIncome(userProfile);
        double incomeMultiplier = calculateIncomeMultiplier(annualIncome);
        BigDecimal baseLimit = annualIncome.multiply(BigDecimal.valueOf(incomeMultiplier))
                .setScale(0, RoundingMode.DOWN);

        // 2. 计算各调整系数
        double gradeFactor = calculateGradeFactor(creditGrade);
        double employmentFactor = calculateEmploymentFactor(userProfile);
        double dtiFactor = calculateDtiFactor(userProfile);
        double riskFactor = calculateRiskFactor(riskScore);

        // 3. 计算最终额度
        BigDecimal finalLimit = baseLimit
                .multiply(BigDecimal.valueOf(gradeFactor))
                .multiply(BigDecimal.valueOf(employmentFactor))
                .multiply(BigDecimal.valueOf(dtiFactor))
                .multiply(BigDecimal.valueOf(riskFactor))
                .setScale(0, RoundingMode.DOWN);

        // 4. 应用上下限
        if (finalLimit.compareTo(properties.getMinLimit()) < 0) {
            finalLimit = properties.getMinLimit();
        } else if (finalLimit.compareTo(properties.getMaxLimit()) > 0) {
            finalLimit = properties.getMaxLimit();
        }

        // 5. 生成说明
        String explanation = generateExplanation(incomeMultiplier, gradeFactor, employmentFactor,
                dtiFactor, riskFactor, baseLimit, finalLimit);

        return new CreditLimitDetail(
                baseLimit,
                incomeMultiplier,
                gradeFactor,
                employmentFactor,
                dtiFactor,
                riskFactor,
                finalLimit,
                explanation
        );
    }

    /**
     * 获取年收入
     */
    private BigDecimal getAnnualIncome(UserProfile profile) {
        if (profile == null || profile.getAnnualIncome() == null) {
            return new BigDecimal("60000");
        }
        return profile.getAnnualIncome();
    }

    /**
     * 计算收入倍数
     * 收入越高，倍数越高，但有上限
     */
    private double calculateIncomeMultiplier(BigDecimal annualIncome) {
        double income = annualIncome.doubleValue();

        if (income < 30000) {
            return properties.getIncomeMultiplierLow();
        } else if (income < 60000) {
            return 0.4;
        } else if (income < 120000) {
            return properties.getIncomeMultiplierMedium();
        } else if (income < 240000) {
            return properties.getIncomeMultiplierHigh();
        } else if (income < 500000) {
            return 0.7;
        } else {
            return 0.8;
        }
    }

    /**
     * 计算信用等级系数（从配置获取）
     */
    private double calculateGradeFactor(String grade) {
        if (grade == null || grade.isEmpty()) {
            return 1.0;
        }
        return properties.getGradeMultipliers()
                .getOrDefault(grade.toUpperCase(), 1.0);
    }

    /**
     * 计算就业年限系数（从配置获取）
     */
    private double calculateEmploymentFactor(UserProfile profile) {
        if (profile == null || profile.getEmploymentYears() == null) {
            return 1.0;
        }
        int years = profile.getEmploymentYears();
        String key;
        if (years < 1) {
            key = "less-than-1";
        } else if (years < 3) {
            key = "1-to-3";
        } else if (years < 5) {
            key = "3-to-5";
        } else if (years < 10) {
            key = "5-to-10";
        } else {
            key = "over-10";
        }
        return properties.getEmploymentYearMultipliers().getOrDefault(key, 1.0);
    }

    /**
     * 计算DTI调整系数（从配置获取）
     */
    private double calculateDtiFactor(UserProfile profile) {
        if (profile == null || profile.getDti() == null) {
            return 1.0;
        }

        double dti = profile.getDti().doubleValue();
        String key;
        if (dti < 0.3) {
            key = "low";
        } else if (dti < 0.5) {
            key = "medium";
        } else if (dti < 0.7) {
            key = "high";
        } else {
            key = "very-high";
        }
        return properties.getDtiAdjustments().getOrDefault(key, 1.0);
    }

    /**
     * 计算风险调整系数
     * 风险评分越低（越安全），系数越高
     *
     * 评分范围 0-100：
     * - 0-30 (低风险): 系数 1.2-1.5
     * - 30-60 (中风险): 系数 0.8-1.2
     * - 60-100 (高风险): 系数 0.5-0.8
     */
    private double calculateRiskFactor(double riskScore) {
        if (riskScore < 20) {
            return 1.5;
        } else if (riskScore < 30) {
            return 1.3;
        } else if (riskScore < 40) {
            return 1.15;
        } else if (riskScore < 50) {
            return 1.0;
        } else if (riskScore < 60) {
            return 0.9;
        } else if (riskScore < 70) {
            return 0.75;
        } else if (riskScore < 80) {
            return 0.6;
        } else {
            return 0.5;
        }
    }

    /**
     * 生成额度说明
     */
    private String generateExplanation(double incomeMultiplier, double gradeFactor,
                                        double employmentFactor, double dtiFactor,
                                        double riskFactor,
                                        BigDecimal baseLimit, BigDecimal finalLimit) {
        StringBuilder sb = new StringBuilder();

        sb.append("基础额度：年收入×").append(String.format("%.1f", incomeMultiplier))
                .append("=").append(baseLimit).append("元。");

        // 信用等级说明
        if (gradeFactor > 1.0) {
            sb.append("信用等级优秀，额度提升").append(String.format("%.0f", (gradeFactor - 1) * 100)).append("%；");
        } else if (gradeFactor < 1.0) {
            sb.append("信用等级较低，额度降低").append(String.format("%.0f", (1 - gradeFactor) * 100)).append("%；");
        }

        // 就业年限说明
        if (employmentFactor > 1.0) {
            sb.append("就业年限较长，额度提升").append(String.format("%.0f", (employmentFactor - 1) * 100)).append("%；");
        } else if (employmentFactor < 1.0) {
            sb.append("就业年限较短，额度调整").append(String.format("%.0f", (1 - employmentFactor) * 100)).append("%；");
        }

        // DTI说明
        if (dtiFactor < 1.0) {
            sb.append("考虑到现有负债，额度限制为").append(String.format("%.0f", dtiFactor * 100)).append("%；");
        }

        // 风险说明
        if (riskFactor >= 1.2) {
            sb.append("风险评分优秀，额度提升").append(String.format("%.0f", (riskFactor - 1) * 100)).append("%；");
        } else if (riskFactor < 0.9) {
            sb.append("风险评分较高，额度降低").append(String.format("%.0f", (1 - riskFactor) * 100)).append("%；");
        }

        sb.append("最终额度：").append(finalLimit).append("元。");

        return sb.toString();
    }
}
