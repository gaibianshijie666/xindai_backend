package com.xindai.xindai.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Operation log view object
 */
@Data
public class OperationLogVO {
    private Long id;
    private Long userId;
    private String userType;
    private String username;
    private String module;
    private String operation;
    private String targetType;
    private String targetId;
    private String detail;
    private String ipAddress;
    private String traceId;
    private LocalDateTime createdAt;
}
