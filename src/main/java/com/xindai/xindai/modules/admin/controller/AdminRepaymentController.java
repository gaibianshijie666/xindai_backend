package com.xindai.xindai.modules.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.dto.ManualRepayDTO;
import com.xindai.xindai.modules.admin.dto.RepaymentAdjustDTO;
import com.xindai.xindai.modules.admin.dto.RepaymentQueryDTO;
import com.xindai.xindai.modules.admin.service.AdminRepaymentService;
import com.xindai.xindai.modules.admin.vo.AdminRepaymentVO;
import com.xindai.xindai.modules.admin.vo.OverdueRepaymentVO;
import com.xindai.xindai.modules.admin.vo.RepaymentStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端还款管理控制器
 */
@Tag(name = "管理端-还款管理", description = "管理端还款管理相关接口")
@RestController
@RequestMapping("/api/v1/admin/repayments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRepaymentController {

    private final AdminRepaymentService adminRepaymentService;

    @Operation(summary = "获取还款计划列表")
    @GetMapping
    public Result<Page<AdminRepaymentVO>> getRepaymentList(RepaymentQueryDTO queryDTO) {
        return Result.success(adminRepaymentService.getRepaymentList(queryDTO));
    }

    @Operation(summary = "获取逾期还款列表")
    @GetMapping("/overdue")
    public Result<List<OverdueRepaymentVO>> getOverdueRepayments() {
        return Result.success(adminRepaymentService.getOverdueRepayments());
    }

    @Operation(summary = "调整罚息")
    @PutMapping("/{id}/adjust")
    @OperateLog(module = "还款管理", operation = "调整罚息")
    public Result<Void> adjustPenalty(
            @Parameter(description = "还款计划ID") @PathVariable Long id,
            @Valid @RequestBody RepaymentAdjustDTO adjustDTO) {
        adminRepaymentService.adjustPenalty(id, adjustDTO);
        return Result.success();
    }

    @Operation(summary = "手动还款")
    @PostMapping("/{id}/manual-repay")
    @OperateLog(module = "还款管理", operation = "手动还款")
    public Result<Void> manualRepay(
            @Parameter(description = "还款计划ID") @PathVariable Long id,
            @Valid @RequestBody ManualRepayDTO repayDTO) {
        adminRepaymentService.manualRepay(id, repayDTO);
        return Result.success();
    }

    @Operation(summary = "获取还款统计")
    @GetMapping("/stats")
    public Result<RepaymentStatsVO> getRepaymentStats() {
        return Result.success(adminRepaymentService.getRepaymentStats());
    }
}
