package com.xindai.xindai.modules.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "完成任务请求")
public class CompleteTaskDTO {

    @NotBlank(message = "任务ID不能为空")
    @Schema(description = "任务ID", example = "12345")
    private String taskId;

    @Schema(description = "流程变量")
    private Map<String, Object> variables;

    @NotBlank(message = "操作类型不能为空")
    @Schema(description = "操作类型：approve-通过, reject-拒绝, return-退回", example = "approve")
    private String action;
}
