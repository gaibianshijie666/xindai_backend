package com.xindai.xindai.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * System config view object
 */
@Data
public class ConfigVO {
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private String category;
    private LocalDateTime updatedAt;
}
