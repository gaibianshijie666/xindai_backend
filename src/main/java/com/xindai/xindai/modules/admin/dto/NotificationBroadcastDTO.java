package com.xindai.xindai.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Broadcast notification DTO
 */
@Data
public class NotificationBroadcastDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotBlank(message = "类型不能为空")
    private String type;
}
