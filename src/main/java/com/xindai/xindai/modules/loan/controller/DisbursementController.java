package com.xindai.xindai.modules.loan.controller;

import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.loan.dto.DisbursementVO;
import com.xindai.xindai.modules.loan.service.DisbursementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端放款控制器
 */
@Tag(name = "管理端-放款管理", description = "管理端放款相关接口")
@RestController
@RequestMapping("/api/v1/admin/disbursements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class DisbursementController {

    private final DisbursementService disbursementService;

    @Operation(summary = "获取放款记录列表")
    @GetMapping
    public Result<List<DisbursementVO>> getDisbursementRecords(
            @Parameter(description = "状态过滤：0=待放款,1=放款中,2=已放款,3=放款失败")
            @RequestParam(required = false) Integer status) {
        return Result.success(disbursementService.getDisbursementRecords(status));
    }

    @Operation(summary = "执行放款")
    @PostMapping("/{disbursementId}/execute")
    @OperateLog(module = "放款管理", operation = "执行放款")
    public Result<Void> executeDisbursement(
            @Parameter(description = "放款记录ID") @PathVariable Long disbursementId) {
        disbursementService.executeDisbursement(disbursementId);
        return Result.success();
    }
}
