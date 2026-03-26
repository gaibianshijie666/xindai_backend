package com.xindai.xindai.modules.admin.controller;

import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.admin.dto.AdminLoginDTO;
import com.xindai.xindai.modules.admin.dto.AdminRegisterDTO;
import com.xindai.xindai.modules.admin.service.AdminAuthService;
import com.xindai.xindai.modules.admin.vo.AdminLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员认证", description = "管理员登录、注册接口")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(
            summary = "管理员登录",
            description = "使用用户名和密码登录管理员账号"
    )
    @ApiResponse(responseCode = "200", description = "登录成功")
    @ApiResponse(responseCode = "400", description = "用户名或密码错误")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "管理员登录信息",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "username": "admin",
                                      "password": "admin123"
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody AdminLoginDTO dto) {
        return Result.success(adminAuthService.login(dto));
    }

    @Operation(
            summary = "管理员注册",
            description = "注册新的管理员账号（需要超级管理员权限）"
    )
    @ApiResponse(responseCode = "200", description = "注册成功")
    @ApiResponse(responseCode = "400", description = "用户名已存在")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/register")
    public Result<AdminLoginVO> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "管理员注册信息",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "username": "admin",
                                      "password": "admin123",
                                      "realName": "管理员",
                                      "phone": "13800138000"
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody AdminRegisterDTO dto) {
        return Result.success(adminAuthService.register(dto));
    }
}
