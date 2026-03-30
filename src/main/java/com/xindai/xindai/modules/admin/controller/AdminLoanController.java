package com.xindai.xindai.modules.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.dto.ContractAdjustDTO;
import com.xindai.xindai.modules.admin.dto.ContractQueryDTO;
import com.xindai.xindai.modules.admin.dto.DisbursementQueryDTO;
import com.xindai.xindai.modules.admin.dto.DisbursementRejectDTO;
import com.xindai.xindai.modules.admin.service.AdminLoanService;
import com.xindai.xindai.modules.admin.vo.AdminContractDetailVO;
import com.xindai.xindai.modules.admin.vo.AdminContractVO;
import com.xindai.xindai.modules.admin.vo.AdminDisbursementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端贷款管理控制器
 */
@Tag(name = "管理端-贷款管理", description = "管理端贷款合同和放款管理接口")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLoanController {

    private final AdminLoanService adminLoanService;

    // ==================== 合同管理 ====================

    @Operation(summary = "获取借款合同列表")
    @GetMapping("/contracts")
    public Result<Page<AdminContractVO>> getContractList(ContractQueryDTO queryDTO) {
        return Result.success(adminLoanService.getContractList(queryDTO));
    }

    @Operation(summary = "获取借款合同详情")
    @GetMapping("/contracts/{id}")
    public Result<AdminContractDetailVO> getContractDetail(
            @Parameter(description = "合同ID") @PathVariable Long id) {
        return Result.success(adminLoanService.getContractDetail(id));
    }

    @Operation(summary = "取消借款合同")
    @PutMapping("/contracts/{id}/cancel")
    @OperateLog(module = "贷款管理", operation = "取消借款合同")
    public Result<Void> cancelContract(
            @Parameter(description = "合同ID") @PathVariable Long id) {
        adminLoanService.cancelContract(id);
        return Result.success();
    }

    @Operation(summary = "调整合同利率")
    @PutMapping("/contracts/{id}/adjust-rate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperateLog(module = "贷款管理", operation = "调整合同利率")
    public Result<Void> adjustContractRate(
            @Parameter(description = "合同ID") @PathVariable Long id,
            @Valid @RequestBody ContractAdjustDTO adjustDTO) {
        adminLoanService.adjustContractRate(id, adjustDTO);
        return Result.success();
    }

    // ==================== 放款管理 ====================

    @Operation(summary = "获取放款记录列表")
    @GetMapping("/disbursements")
    public Result<Page<AdminDisbursementVO>> getDisbursementList(DisbursementQueryDTO queryDTO) {
        return Result.success(adminLoanService.getDisbursementList(queryDTO));
    }

    @Operation(summary = "执行放款")
    @PostMapping("/disbursements/{id}/execute")
    @OperateLog(module = "放款管理", operation = "执行放款")
    public Result<Void> executeDisbursement(
            @Parameter(description = "放款记录ID") @PathVariable Long id) {
        adminLoanService.executeDisbursement(id);
        return Result.success();
    }

    @Operation(summary = "拒绝放款")
    @PostMapping("/disbursements/{id}/reject")
    @OperateLog(module = "放款管理", operation = "拒绝放款")
    public Result<Void> rejectDisbursement(
            @Parameter(description = "放款记录ID") @PathVariable Long id,
            @Valid @RequestBody DisbursementRejectDTO rejectDTO) {
        adminLoanService.rejectDisbursement(id, rejectDTO);
        return Result.success();
    }
}
