package com.xindai.xindai.modules.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.dto.ApplicationQueryDTO;
import com.xindai.xindai.modules.admin.dto.ApplicationReviewDTO;
import com.xindai.xindai.modules.admin.service.AdminApplicationService;
import com.xindai.xindai.modules.admin.vo.AdminApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端借款申请控制器
 */
@Tag(name = "管理端-借款审核", description = "管理端借款审核相关接口")
@RestController
@RequestMapping("/api/v1/admin/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApplicationController {

    private final AdminApplicationService adminApplicationService;

    @Operation(summary = "获取借款申请列表")
    @GetMapping
    public Result<Page<AdminApplicationVO>> getApplicationList(ApplicationQueryDTO queryDTO) {
        return Result.success(adminApplicationService.getApplicationList(queryDTO));
    }

    @Operation(summary = "审核借款申请")
    @PostMapping("/{id}/review")
    @OperateLog(module = "贷款审核", operation = "审核贷款申请")
    public Result<Void> reviewApplication(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @Valid @RequestBody ApplicationReviewDTO reviewDTO) {
        adminApplicationService.reviewApplication(id, reviewDTO);
        return Result.success();
    }
}
