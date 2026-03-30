package com.xindai.xindai.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Targeted notification DTO
 */
@Data
public class NotificationTargetedDTO {

    @NotEmpty(message = "用户列表不能为空")
    private List<Long> userIds;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotBlank(message = "类型不能为空")
    private String type;
}
