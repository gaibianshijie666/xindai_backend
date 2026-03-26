package com.xindai.xindai.modules.enterprise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("enterprise_operation_log")
public class EnterpriseOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("enterprise_id")
    private Long enterpriseId;

    @TableField("user_id")
    private Long userId;

    @TableField("operation_type")
    private String operationType;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private Long targetId;

    private String detail;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
