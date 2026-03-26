package com.xindai.xindai.modules.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "任务视图对象")
public class TaskVO {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "处理人")
    private String assignee;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "流程变量")
    private Map<String, Object> variables;
}
