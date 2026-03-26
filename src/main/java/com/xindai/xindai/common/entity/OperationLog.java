package com.xindai.xindai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("user_type")
    private String userType;

    private String username;

    private String module;

    private String operation;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private String targetId;

    private String detail;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("trace_id")
    private String traceId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
