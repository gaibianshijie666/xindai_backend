package com.xindai.xindai.modules.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * System configuration entity
 */
@Data
@TableName("system_config")
public class SystemConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    private String description;

    private String category;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
