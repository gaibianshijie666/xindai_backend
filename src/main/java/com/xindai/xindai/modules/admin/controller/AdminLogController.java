package com.xindai.xindai.modules.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.dto.OperationLogQueryDTO;
import com.xindai.xindai.modules.admin.service.AdminLogService;
import com.xindai.xindai.modules.admin.vo.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin operation log controller
 */
@Tag(name = "管理端-操作日志", description = "管理端操作日志接口")
@RestController
@RequestMapping("/api/v1/admin/operation-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private final AdminLogService adminLogService;

    @Operation(summary = "获取操作日志列表（分页，支持过滤）")
    @GetMapping
    public Result<Page<OperationLogVO>> getOperationLogs(@Valid OperationLogQueryDTO query) {
        return Result.success(adminLogService.getOperationLogs(query));
    }

    @Operation(summary = "获取操作日志详情")
    @GetMapping("/{id}")
    public Result<OperationLogVO> getOperationLogDetail(@PathVariable Long id) {
        return Result.success(adminLogService.getOperationLogDetail(id));
    }
}
