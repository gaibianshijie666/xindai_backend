package com.xindai.xindai.modules.enterprise.controller;

import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.enterprise.dto.CreditApplyDTO;
import com.xindai.xindai.modules.enterprise.dto.CreditInfoVO;
import com.xindai.xindai.modules.enterprise.service.EnterpriseCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/enterprise/credit")
@Tag(name = "企业端-额度管理")
@RequiredArgsConstructor
public class EnterpriseCreditController {
    private final EnterpriseCreditService enterpriseCreditService;

    @GetMapping
    @Operation(summary = "查询企业额度信息")
    @PreAuthorize("hasAnyRole('ENTERPRISE_ADMIN', 'ENTERPRISE_OPERATOR')")
    public Result<CreditInfoVO> getCreditInfo(@RequestAttribute("enterpriseId") Long enterpriseId) {
        return Result.success(enterpriseCreditService.getCreditInfo(enterpriseId));
    }

    @PostMapping("/apply")
    @Operation(summary = "申请额度提升")
    @PreAuthorize("hasRole('ENTERPRISE_ADMIN')")
    public Result<Void> applyCreditIncrease(@RequestBody @Valid CreditApplyDTO dto,
                                            @RequestAttribute("enterpriseId") Long enterpriseId) {
        enterpriseCreditService.applyCreditIncrease(enterpriseId, dto);
        return Result.success();
    }
}
