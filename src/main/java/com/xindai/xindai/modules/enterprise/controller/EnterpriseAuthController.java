package com.xindai.xindai.modules.enterprise.controller;

import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseLoginDTO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseUserVO;
import com.xindai.xindai.modules.enterprise.dto.PasswordChangeDTO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseInfoVO;
import com.xindai.xindai.modules.enterprise.service.EnterpriseAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 企业认证控制器
 */
@Tag(name = "企业认证", description = "企业用户登录认证接口")
@RestController
@RequestMapping("/api/v1/enterprise")
@RequiredArgsConstructor
public class EnterpriseAuthController {

    private final EnterpriseAuthService enterpriseAuthService;

    @Operation(summary = "企业用户登录")
    @PostMapping("/auth/login")
    public Result<EnterpriseUserVO> login(@Valid @RequestBody EnterpriseLoginDTO dto) {
        return Result.success(enterpriseAuthService.login(dto));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/auth/profile")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public Result<EnterpriseUserVO> profile(
            @RequestAttribute(value = "enterpriseId", required = false) Long enterpriseId,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(enterpriseAuthService.getUserProfile(userId, enterpriseId));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/auth/password")
    @PreAuthorize("hasRole('ENTERPRISE')")
    @OperateLog(module = "企业认证", operation = "修改密码")
    public Result<Void> changePassword(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody PasswordChangeDTO dto) {
        enterpriseAuthService.changePassword(userId, dto);
        return Result.success();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/auth/logout")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public Result<Void> logout(@RequestAttribute("userId") Long userId) {
        enterpriseAuthService.logout(userId);
        return Result.success();
    }

    @Operation(summary = "获取企业信息")
    @GetMapping("/info")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public Result<EnterpriseInfoVO> getEnterpriseInfo(
            @RequestAttribute("enterpriseId") Long enterpriseId) {
        return Result.success(enterpriseAuthService.getEnterpriseInfoVO(enterpriseId));
    }
}
