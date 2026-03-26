package com.xindai.xindai.modules.loan.service;

import com.xindai.xindai.modules.loan.entity.CreditLimit;
import com.xindai.xindai.modules.user.entity.UserProfile;
import java.math.BigDecimal;

/**
 * 智能信用额度计算器
 *
 * 基于多因子综合评估用户可贷额度：
 * 1. 收入因子 - 基础额度 = 年收入 × 倍数
 * 2. 风险因子 - 根据风险评分调整
 * 3. 信用因子 - 根据信用等级调整
 * 4. 负债因子 - 根据债务收入比限制
 */
public interface CreditLimitCalculator {

    /**
     * 计算用户信用额度
     *
     * @param userProfile 用户画像（包含收入、信用分等）
     * @param riskScore 风险评分 (0-100)
     * @param creditGrade 信用等级 (A-G)
     * @return 计算后的信用额度
     */
    CreditLimit calculate(UserProfile userProfile, double riskScore, String creditGrade);

    /**
     * 根据风险评分调整现有额度
     *
     * @param currentLimit 当前额度
     * @param riskScore 风险评分
     * @return 调整后的额度
     */
    BigDecimal adjustByRiskScore(BigDecimal currentLimit, double riskScore);

    /**
     * 获取额度计算详情（用于展示给用户）
     */
    CreditLimitDetail getCalculationDetail(UserProfile userProfile, double riskScore, String creditGrade);

    /**
     * 额度计算详情
     */
    record CreditLimitDetail(
        BigDecimal baseLimit,          // 基础额度（基于收入）
        double incomeMultiplier,       // 收入倍数
        double gradeFactor,            // 信用等级系数
        double employmentFactor,       // 就业年限系数
        double dtiFactor,              // DTI调整系数
        double riskAdjustFactor,       // 风险调整系数
        BigDecimal finalLimit,         // 最终额度
        String explanation             // 额度说明
    ) {}
}
