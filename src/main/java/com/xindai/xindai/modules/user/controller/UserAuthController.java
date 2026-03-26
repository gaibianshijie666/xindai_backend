package com.xindai.xindai.modules.user.controller;

import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.user.dto.UserLoginDTO;
import com.xindai.xindai.modules.user.dto.UserRegisterDTO;
import com.xindai.xindai.modules.user.dto.UserVO;
import com.xindai.xindai.modules.user.service.UserAuthService;
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
public class UserAuthController {

    private final UserAuthService userAuthService;

    @Operation(
            summary = "用户注册",
            description = "使用手机号和密码注册新用户账号，注册成功后自动登录并返回用户信息和Token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "success",
                                      "data": {
                                        "id": 1,
                                        "phone": "13800138000",
                                        "token": "eyJhbGciOiJIUzI1NiIs..."
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "手机号已注册或参数格式错误")
    })
    @PostMapping("/register")
    public Result<UserVO> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户注册信息",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "phone": "13800138000",
                                      "password": "Password123"
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody UserRegisterDTO dto) {
        return Result.success(userAuthService.register(dto));
    }

    @Operation(
            summary = "用户登录",
            description = "使用手机号和密码登录，成功后返回用户信息和JWT Token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "400", description = "手机号或密码错误"),
            @ApiResponse(responseCode = "403", description = "账号已被禁用")
    })
    @PostMapping("/login")
    public Result<UserVO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户登录信息",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "phone": "13800138000",
                                      "password": "Password123"
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody UserLoginDTO dto) {
        return Result.success(userAuthService.login(dto));
    }

    @Operation(
            summary = "退出登录",
            description = "退出当前登录状态，使Token失效",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/logout")
    public Result<Void> logout(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        userAuthService.logout(userId);
        return Result.success();
    }
}
