package com.xindai.xindai.modules.workflow.controller;

import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.workflow.dto.CompleteTaskDTO;
import com.xindai.xindai.modules.workflow.dto.TaskVO;
import com.xindai.xindai.modules.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端工作流控制器
 */
@Tag(name = "管理端-工作流管理", description = "流程任务管理相关接口")
@RestController
@RequestMapping("/api/v1/admin/workflow")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class WorkflowController {

    private final WorkflowService workflowService;

    @Operation(summary = "获取待办任务列表")
    @GetMapping("/tasks")
    public Result<List<TaskVO>> getPendingTasks(
            @Parameter(description = "处理人", example = "admin")
            @RequestParam String assignee) {
        return Result.success(workflowService.getPendingTasks(assignee));
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/tasks/{taskId}")
    public Result<TaskVO> getTaskDetail(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        return Result.success(workflowService.getTaskDetail(taskId));
    }

    @Operation(summary = "完成任务")
    @PostMapping("/tasks/{taskId}/complete")
    @OperateLog(module = "工作流管理", operation = "完成任务")
    public Result<Void> completeTask(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Valid @RequestBody CompleteTaskDTO dto) {
        // 将 action 转为流程变量 reviewResult
        Map<String, Object> variables = new HashMap<>();
        if (dto.getVariables() != null) {
            variables.putAll(dto.getVariables());
        }
        variables.put("reviewResult", dto.getAction().toUpperCase());

        workflowService.completeTask(taskId, variables);
        return Result.success();
    }

    @Operation(summary = "查询流程状态")
    @GetMapping("/process/{processInstanceId}/status")
    public Result<String> getProcessStatus(
            @Parameter(description = "流程实例ID") @PathVariable String processInstanceId) {
        return Result.success(workflowService.getProcessStatus(processInstanceId));
    }
}
