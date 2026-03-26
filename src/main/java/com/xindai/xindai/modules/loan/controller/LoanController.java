package com.xindai.xindai.modules.loan.controller;

import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.loan.dto.*;
import com.xindai.xindai.modules.loan.service.LoanService;
import com.xindai.xindai.modules.loan.service.OverdueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "借贷管理", description = "额度查询、借款申请、申请记录、还款等接口")
@RestController
@RequestMapping("/api/v1/loan")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService loanService;
    private final OverdueService overdueService;

    @Operation(
            summary = "查询信用额度",
            description = "获取当前用户的信用额度信息，包括总额度、已用额度、可用额度等"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "success",
                                      "data": {
                                        "id": 1,
                                        "userId": 1,
                                        "totalLimit": 50000.00,
                                        "usedLimit": 10000.00,
                                        "availableLimit": 40000.00,
                                        "status": 1,
                                        "baseLimit": 30000.00,
                                        "riskAdjustFactor": 1.2,
                                        "explanation": "基于您的信用状况，系统为您评定了此额度"
                                      }
                                    }
                                    """)
                    ))
    })
    @GetMapping("/limit")
    public Result<CreditLimitVO> getLimit(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return Result.success(loanService.getCreditLimit(userId));
    }

    @Operation(
            summary = "申请提额",
            description = "申请提升信用额度，需要有至少3笔已结清的借款记录"
    )
    @PostMapping("/limit/apply")
    @OperateLog(module = "借贷管理", operation = "申请提额")
    public Result<CreditLimitVO> applyLimitIncrease(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody LimitApplyDTO dto) {
        return Result.success(loanService.applyLimitIncrease(userId, dto));
    }

    @Operation(
            summary = "额度预估",
            description = "根据收入、工作年限等信息预估可贷额度"
    )
    @PostMapping("/limit/estimate")
    public Result<LimitEstimateVO> estimateLimit(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody LimitEstimateDTO dto) {
        return Result.success(loanService.estimateLimit(userId, dto));
    }

    @Operation(
            summary = "提交借款申请",
            description = "提交新的借款申请，系统将自动进行风控评估并返回申请结果"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "申请提交成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败"),
            @ApiResponse(responseCode = "403", description = "额度不足")
    })
    @PostMapping("/apply")
    @OperateLog(module = "借贷管理", operation = "提交借款申请")
    public Result<LoanApplicationVO> apply(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "借款申请信息",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "amount": 10000.00,
                                      "term": 12,
                                      "purpose": "消费"
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody LoanApplyDTO dto) {
        return Result.success(loanService.apply(userId, dto));
    }

    @Operation(
            summary = "获取借款申请列表",
            description = "获取当前用户的所有借款申请记录，按时间倒序排列"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/applications")
    public Result<List<LoanApplicationVO>> getApplications(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return Result.success(loanService.getApplications(userId));
    }

    @Operation(
            summary = "获取借款申请详情",
            description = "根据申请ID获取借款申请的详细信息"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "申请记录不存在")
    })
    @GetMapping("/applications/{id}")
    public Result<LoanApplicationVO> getApplicationDetail(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "申请ID", required = true, example = "1")
            @PathVariable Long id) {
        return Result.success(loanService.getApplicationDetail(userId, id));
    }

    @Operation(
            summary = "获取待还款列表",
            description = "获取当前用户的所有待还款记录"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/pending-repayment")
    public Result<List<RepaymentPlanVO>> getPendingRepayment(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return Result.success(loanService.getPendingRepayment(userId));
    }

    @Operation(
            summary = "获取借款合同列表",
            description = "获取当前用户的所有借款合同"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/contracts")
    public Result<List<LoanContractVO>> getContracts(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return Result.success(loanService.getContracts(userId));
    }

    @Operation(
            summary = "获取合同详情",
            description = "根据合同ID获取借款合同的详细信息"
    )
    @GetMapping("/contracts/{id}")
    public Result<LoanContractVO> getContractDetail(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "合同ID", required = true, example = "1")
            @PathVariable Long id) {
        return Result.success(loanService.getContractDetail(userId, id));
    }

    @Operation(
            summary = "获取还款计划",
            description = "根据合同ID获取该合同的所有还款计划"
    )
    @GetMapping("/repayment-plan/{contractId}")
    public Result<List<RepaymentPlanVO>> getRepaymentPlanByContract(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "合同ID", required = true, example = "1")
            @PathVariable Long contractId) {
        return Result.success(loanService.getRepaymentPlansByContract(userId, contractId));
    }

    @Operation(
            summary = "获取所有还款计划",
            description = "获取当前用户所有合同的还款计划"
    )
    @GetMapping("/repayment-plans")
    public Result<List<RepaymentPlanVO>> getAllRepaymentPlans(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return Result.success(loanService.getAllRepaymentPlans(userId));
    }

    @Operation(
            summary = "还款",
            description = "对指定合同进行还款，可还一期或多期"
    )
    @PostMapping("/repay")
    @OperateLog(module = "借贷管理", operation = "还款")
    public Result<RepaymentResultVO> repay(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody RepayDTO dto) {
        return Result.success(loanService.repay(userId, dto));
    }

    @Operation(
            summary = "按期数还款",
            description = "还清指定合同的指定期数"
    )
    @PostMapping("/repay-period")
    public Result<RepaymentResultVO> repayByPeriod(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "合同ID", required = true, example = "1")
            @RequestParam Long contractId,
            @Parameter(description = "期数", required = true, example = "1")
            @RequestParam Integer period) {
        return Result.success(loanService.repayByPeriod(userId, contractId, period));
    }

    @Operation(summary = "获取逾期还款列表")
    @GetMapping("/overdue")
    public Result<List<OverdueInfoVO>> getOverdueList(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return Result.success(overdueService.getOverdueList(userId));
    }
}
