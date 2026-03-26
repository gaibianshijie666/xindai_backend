package com.xindai.xindai.modules.loan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 放款状态枚举
 */
@Getter
@AllArgsConstructor
public enum DisbursementStatus {

    PENDING(0, "待放款"),
    PROCESSING(1, "放款中"),
    COMPLETED(2, "已放款"),
    FAILED(3, "放款失败");

    private final int code;
    private final String desc;

    public static DisbursementStatus fromCode(int code) {
        for (DisbursementStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown disbursement status code: " + code);
    }
}
