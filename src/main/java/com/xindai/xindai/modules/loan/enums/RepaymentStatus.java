package com.xindai.xindai.modules.loan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RepaymentStatus {

    PENDING(0, "待还款"),
    PAID(1, "已还款"),
    OVERDUE(2, "已逾期");

    private final int code;
    private final String desc;

    public static RepaymentStatus fromCode(int code) {
        for (RepaymentStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown repayment status code: " + code);
    }
}
