package com.xindai.xindai.modules.user.controller;

import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.user.dto.UserProfileUpdateDTO;
import com.xindai.xindai.modules.user.dto.UserProfileVO;
import com.xindai.xindai.modules.user.service.UserProfileDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理", description = "用户注册、登录、个人信息、实名认证等接口")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserProfileDetailController {

    private final UserProfileDetailService userProfileDetailService;

    @Operation(
            summary = "获取用户画像详情",
            description = "获取当前用户的画像信息，包括年收入、就业年限、风险评分等",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/profile/detail")
    public Result<UserProfileVO> getProfileDetail(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return Result.success(userProfileDetailService.getProfileDetail(userId));
    }

    @Operation(
            summary = "更新用户画像信息",
            description = "更新用户的画像信息，包括年收入、就业年限、工作单位等，用于风控评估",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/profile/detail")
    public Result<UserProfileVO> updateProfileDetail(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UserProfileUpdateDTO dto) {
        return Result.success(userProfileDetailService.updateProfileDetail(userId, dto));
    }
}
