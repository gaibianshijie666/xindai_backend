package com.xindai.xindai.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Config update DTO
 */
@Data
public class ConfigUpdateDTO {

    @NotEmpty(message = "配置列表不能为空")
    private List<ConfigItem> items;

    @Data
    public static class ConfigItem {
        @NotBlank(message = "配置键不能为空")
        private String key;

        @NotBlank(message = "配置值不能为空")
        private String value;

        private String reason;
    }
}
