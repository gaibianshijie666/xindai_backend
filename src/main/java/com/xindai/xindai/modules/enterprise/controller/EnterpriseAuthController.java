package com.xindai.xindai.modules.enterprise.controller;

import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseLoginDTO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseUserVO;
import com.xindai.xindai.modules.enterprise.dto.PasswordChangeDTO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseInfoVO;
import com.xindai.xindai.modules.enterprise.entity.Enterprise;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseUser;
import com.xindai.xindai.modules.enterprise.service.EnterpriseAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public Result<EnterpriseUserVO> profile(
            @RequestAttribute(value = "enterpriseId", required = false) Long enterpriseId,
            @RequestAttribute(value = "userId", required = false) Long userId) {

        EnterpriseUser user = enterpriseAuthService.getCurrentUser(userId);
        Enterprise enterprise = enterpriseAuthService.getEnterprise(enterpriseId);

        EnterpriseUserVO vo = new EnterpriseUserVO();
        vo.setId(user.getId());
        vo.setEnterpriseId(enterprise.getId());
        vo.setEnterpriseName(enterprise.getName());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());

        return Result.success(vo);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/auth/password")
    public Result<Void> changePassword(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody PasswordChangeDTO dto) {
        enterpriseAuthService.changePassword(userId, dto);
        return Result.success();
    }

    @Operation(summary = "获取企业信息")
    @GetMapping("/info")
    public Result<EnterpriseInfoVO> getEnterpriseInfo(
            @RequestAttribute("enterpriseId") Long enterpriseId) {
        Enterprise enterprise = enterpriseAuthService.getEnterprise(enterpriseId);
        EnterpriseInfoVO vo = new EnterpriseInfoVO();
        vo.setName(enterprise.getName());
        vo.setEnterpriseNo(enterprise.getEnterpriseNo());
        vo.setApiKey(enterprise.getApiKey());
        return Result.success(vo);
    }
}
