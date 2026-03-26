package com.xindai.xindai.modules.collection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.collection.dto.*;
import com.xindai.xindai.modules.collection.service.CollectionTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 催收管理控制器
 */
@Tag(name = "管理端-催收管理", description = "催收任务管理接口")
@RestController
@RequestMapping("/api/v1/admin/collection")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CollectionController {

    private final CollectionTaskService collectionTaskService;

    @Operation(summary = "获取催收任务列表")
    @GetMapping("/tasks")
    public Result<Page<CollectionTaskVO>> getTaskList(CollectionTaskQueryDTO queryDTO) {
        return Result.success(collectionTaskService.getTaskList(queryDTO));
    }

    @Operation(summary = "获取催收任务详情")
    @GetMapping("/tasks/{taskId}")
    public Result<CollectionTaskVO> getTaskDetail(
            @Parameter(description = "催收任务ID") @PathVariable Long taskId) {
        return Result.success(collectionTaskService.getTaskDetail(taskId));
    }

    @Operation(summary = "分配催收任务")
    @PostMapping("/tasks/{taskId}/assign")
    public Result<CollectionTaskVO> assignTask(
            @Parameter(description = "催收任务ID") @PathVariable Long taskId,
            @Parameter(description = "催收员ID") @RequestParam Long collectorId) {
        return Result.success(collectionTaskService.assignTask(taskId, collectorId));
    }

    @Operation(summary = "开始处理催收任务")
    @PostMapping("/tasks/{taskId}/start")
    public Result<CollectionTaskVO> startTask(
            @Parameter(description = "催收任务ID") @PathVariable Long taskId) {
        return Result.success(collectionTaskService.startTask(taskId));
    }

    @Operation(summary = "完成催收任务")
    @PostMapping("/tasks/{taskId}/complete")
    public Result<CollectionTaskVO> completeTask(
            @Parameter(description = "催收任务ID") @PathVariable Long taskId) {
        return Result.success(collectionTaskService.completeTask(taskId));
    }

    @Operation(summary = "关闭催收任务")
    @PostMapping("/tasks/{taskId}/close")
    public Result<CollectionTaskVO> closeTask(
            @Parameter(description = "催收任务ID") @PathVariable Long taskId) {
        return Result.success(collectionTaskService.closeTask(taskId));
    }

    @Operation(summary = "添加催收记录")
    @PostMapping("/records")
    public Result<CollectionRecordVO> addRecord(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateCollectionRecordDTO dto) {
        // 使用管理员ID作为催收员ID（从用户详情中解析）
        Long collectorId = Long.parseLong(userDetails.getUsername());
        return Result.success(collectionTaskService.addRecord(collectorId, dto));
    }

    @Operation(summary = "获取催收记录列表")
    @GetMapping("/tasks/{taskId}/records")
    public Result<List<CollectionRecordVO>> getRecords(
            @Parameter(description = "催收任务ID") @PathVariable Long taskId) {
        return Result.success(collectionTaskService.getRecords(taskId));
    }
}
