package com.xindai.xindai.modules.user.controller;

import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.user.dto.*;
import com.xindai.xindai.modules.user.service.UserProfileService;
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

@Tag(name = "用户管理", description = "用户注册、登录、个人信息、实名认证等接口")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(
            summary = "获取当前用户信息",
            description = "获取当前登录用户的详细信息，需要Bearer Token认证",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录或Token已过期")
    })
    @GetMapping("/profile")
    public Result<UserVO> profile(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        var user = userProfileService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setRealName(user.getRealName());
        vo.setIdCard(user.getIdCard());
        vo.setStatus(user.getStatus());
        return Result.success(vo);
    }

    @Operation(
            summary = "更新用户信息",
            description = "更新当前登录用户的个人信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/profile")
    @OperateLog(module = "用户管理", operation = "更新用户信息")
    public Result<UserVO> updateProfile(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UserUpdateDTO dto) {
        return Result.success(userProfileService.updateProfile(userId, dto));
    }

    @Operation(
            summary = "修改密码",
            description = "修改当前登录用户的密码，修改成功后需要重新登录",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/password")
    @OperateLog(module = "用户管理", operation = "修改密码")
    public Result<Void> changePassword(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody PasswordChangeDTO dto) {
        userProfileService.changePassword(userId, dto);
        return Result.success();
    }

    @Operation(
            summary = "实名认证",
            description = "提交真实姓名和身份证号进行实名认证",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/verify-identity")
    @OperateLog(module = "用户管理", operation = "实名认证")
    public Result<UserVO> verifyIdentity(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody VerifyIdentityDTO dto) {
        return Result.success(userProfileService.verifyIdentity(userId, dto));
    }
}
