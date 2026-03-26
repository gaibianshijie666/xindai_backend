package com.xindai.xindai.common.constants;

public final class RiskConstants {
    private RiskConstants() {}

    // 风险评分阈值
    public static final int RISK_SCORE_HIGH_THRESHOLD = 700;
    public static final int RISK_SCORE_MEDIUM_THRESHOLD = 500;
    public static final int RISK_SCORE_LOW_THRESHOLD = 300;

    // 风险等级
    public static final int RISK_LEVEL_LOW = 0;
    public static final int RISK_LEVEL_MEDIUM = 1;
    public static final int RISK_LEVEL_HIGH = 2;

    // 评估类型
    public static final int ASSESSMENT_TYPE_PRE_LOAN = 1;
    public static final int ASSESSMENT_TYPE_POST_LOAN = 2;
    public static final int ASSESSMENT_TYPE_PERIODIC = 3;
}
