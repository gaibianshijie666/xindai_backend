package com.xindai.xindai.modules.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "启动流程请求")
public class StartProcessDTO {

    @NotBlank(message = "流程定义key不能为空")
    @Schema(description = "流程定义key", example = "loanApproval")
    private String processDefinitionKey;

    @Schema(description = "业务主键", example = "APP20260326001")
    private String businessKey;

    @Schema(description = "流程变量")
    private Map<String, Object> variables;
}
