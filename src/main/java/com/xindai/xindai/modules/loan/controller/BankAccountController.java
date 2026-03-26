package com.xindai.xindai.modules.loan.controller;

import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.loan.dto.BankAccountDTO;
import com.xindai.xindai.modules.loan.entity.BankAccount;
import com.xindai.xindai.modules.loan.service.DisbursementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 银行卡管理控制器
 */
@Tag(name = "借贷管理-银行卡", description = "银行卡绑定与管理接口")
@RestController
@RequestMapping("/api/v1/loan/bank-accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BankAccountController {

    private final DisbursementService disbursementService;

    @Operation(summary = "绑定银行卡")
    @PostMapping
    @OperateLog(module = "借贷管理", operation = "绑定银行卡")
    public Result<BankAccount> bindBankAccount(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody BankAccountDTO dto) {
        return Result.success(disbursementService.bindBankAccount(userId, dto));
    }

    @Operation(summary = "获取银行卡列表")
    @GetMapping
    public Result<List<BankAccount>> getBankAccounts(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return Result.success(disbursementService.getBankAccounts(userId));
    }

    @Operation(summary = "获取默认银行卡")
    @GetMapping("/default")
    public Result<BankAccount> getDefaultBankAccount(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return Result.success(disbursementService.getDefaultBankAccount(userId));
    }

    @Operation(summary = "解绑银行卡")
    @DeleteMapping("/{bankAccountId}")
    @OperateLog(module = "借贷管理", operation = "解绑银行卡")
    public Result<Void> unbindBankAccount(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "银行卡ID") @PathVariable Long bankAccountId) {
        disbursementService.unbindBankAccount(userId, bankAccountId);
        return Result.success();
    }
}
