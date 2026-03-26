package com.xindai.xindai.common.constants;

import java.math.BigDecimal;

public final class CreditLimitConstants {
    private CreditLimitConstants() {}

    // 默认额度
    public static final BigDecimal DEFAULT_CREDIT_LIMIT = new BigDecimal("10000");
    public static final BigDecimal MIN_CREDIT_LIMIT = new BigDecimal("1000");
    public static final BigDecimal MAX_CREDIT_LIMIT = new BigDecimal("500000");

    // 额度倍数
    public static final double INCOME_MULTIPLIER_LOW = 0.3;
    public static final double INCOME_MULTIPLIER_MEDIUM = 0.5;
    public static final double INCOME_MULTIPLIER_HIGH = 0.6;
}
