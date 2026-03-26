package com.xindai.xindai.modules.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("user_type")
    private String userType;

    private String title;

    private String content;

    private String type;

    @TableField("is_read")
    private Integer isRead;

    @TableField("related_id")
    private String relatedId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("read_at")
    private LocalDateTime readAt;
}
