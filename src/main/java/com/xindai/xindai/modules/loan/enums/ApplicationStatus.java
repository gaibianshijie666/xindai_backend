package com.xindai.xindai.modules.loan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApplicationStatus {

    PENDING(0, "待审批"),
    REVIEWING(1, "审批中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝"),
    DISBURSED(4, "已放款"),
    SETTLED(5, "已结清");

    private final int code;
    private final String desc;

    public static ApplicationStatus fromCode(int code) {
        for (ApplicationStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ApplicationStatus code: " + code);
    }
}
