package com.xindai.xindai.modules.enterprise.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.enterprise.dto.*;
import com.xindai.xindai.modules.enterprise.service.EnterpriseCustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业客户管理控制器
 */
@Tag(name = "企业客户管理", description = "企业客户增删改查接口")
@RestController
@RequestMapping("/api/v1/enterprise/customers")
@RequiredArgsConstructor
public class EnterpriseCustomerController {

    private final EnterpriseCustomerService customerService;

    @Operation(summary = "获取客户列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ENTERPRISE_ADMIN', 'ENTERPRISE_OPERATOR')")
    public Result<Page<EnterpriseCustomerVO>> list(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            EnterpriseCustomerQueryDTO queryDTO) {
        return Result.success(customerService.list(enterpriseId, queryDTO));
    }

    @Operation(summary = "新增客户")
    @PostMapping
    @PreAuthorize("hasRole('ENTERPRISE_ADMIN')")
    @OperateLog(module = "企业客户管理", operation = "新增客户")
    public Result<EnterpriseCustomerVO> create(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @Valid @RequestBody EnterpriseCustomerDTO dto) {
        return Result.success(customerService.create(enterpriseId, dto));
    }

    @Operation(summary = "更新客户")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ENTERPRISE_ADMIN')")
    @OperateLog(module = "企业客户管理", operation = "更新客户")
    public Result<EnterpriseCustomerVO> update(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @PathVariable Long id,
            @Valid @RequestBody EnterpriseCustomerDTO dto) {
        return Result.success(customerService.update(enterpriseId, id, dto));
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ENTERPRISE_ADMIN')")
    @OperateLog(module = "企业客户管理", operation = "删除客户")
    public Result<Void> delete(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @PathVariable Long id) {
        customerService.delete(enterpriseId, id);
        return Result.success(null);
    }

    @Operation(summary = "批量导入客户")
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ENTERPRISE_ADMIN')")
    @OperateLog(module = "企业客户管理", operation = "批量导入客户")
    public Result<Void> batchImport(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @RequestBody List<@Valid EnterpriseCustomerDTO> customers) {
        customerService.batchImport(enterpriseId, customers);
        return Result.success(null);
    }

    @Operation(summary = "批量删除客户")
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ENTERPRISE_ADMIN')")
    @OperateLog(module = "企业客户管理", operation = "批量删除客户")
    public Result<BatchOperationResultVO> batchDelete(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @Valid @RequestBody BatchIdsDTO dto) {
        return Result.success(customerService.batchDelete(enterpriseId, dto.getIds()));
    }

    @Operation(summary = "批量更新客户状态")
    @PutMapping("/batch/status")
    @PreAuthorize("hasRole('ENTERPRISE_ADMIN')")
    public Result<BatchOperationResultVO> batchUpdateStatus(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @Valid @RequestBody BatchUpdateStatusDTO dto) {
        return Result.success(customerService.batchUpdateStatus(enterpriseId, dto));
    }

    @Operation(summary = "导出客户Excel")
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ENTERPRISE_ADMIN', 'ENTERPRISE_OPERATOR')")
    public void export(
            @RequestAttribute("enterpriseId") Long enterpriseId,
            @RequestParam(required = false) List<Long> ids,
            HttpServletResponse response) {
        customerService.export(enterpriseId, ids, response);
    }
}
