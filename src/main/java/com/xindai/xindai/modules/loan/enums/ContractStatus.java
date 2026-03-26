package com.xindai.xindai.modules.loan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContractStatus {

    PENDING(0, "待放款"),
    REPAYING(1, "还款中"),
    SETTLED(2, "已结清"),
    OVERDUE(3, "逾期");

    private final int code;
    private final String desc;

    public static ContractStatus fromCode(int code) {
        for (ContractStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown contract status code: " + code);
    }
}
