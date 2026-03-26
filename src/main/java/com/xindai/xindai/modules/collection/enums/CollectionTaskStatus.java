package com.xindai.xindai.modules.collection.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 催收任务状态枚举
 */
@Getter
@AllArgsConstructor
public enum CollectionTaskStatus {

    PENDING(0, "待分配"),
    ASSIGNED(1, "已分配"),
    IN_PROGRESS(2, "处理中"),
    COMPLETED(3, "已完成"),
    CLOSED(4, "已关闭");

    private final int code;
    private final String desc;

    public static CollectionTaskStatus fromCode(int code) {
        for (CollectionTaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown CollectionTaskStatus code: " + code);
    }
}
