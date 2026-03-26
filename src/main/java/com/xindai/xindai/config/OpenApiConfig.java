package com.xindai.xindai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 配置类
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        // JWT 认证方案
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("开发服务器")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 认证，格式：Bearer {token}")
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("信贷风控系统 API")
                .description("""
                        ## 多模态智能风控系统 API 文档

                        ### 认证说明
                        - 用户端接口：使用 `/api/v1/user/login` 获取 token
                        - 企业端接口：使用 `/api/v1/enterprise/auth/login` 获取 token
                        - 在请求头中添加：`Authorization: Bearer {token}`

                        ### 业务模块
                        - **用户管理**：注册、登录、个人信息
                        - **借贷管理**：额度查询、借款申请、还款管理
                        - **风控管理**：风险评估、黑名单管理
                        - **企业端**：客户管理、代客申请、数据看板

                        ### 响应格式
                        ```json
                        {
                          "code": 200,
                          "message": "success",
                          "data": { ... }
                        }
                        ```

                        ### 错误码说明
                        | 错误码 | 说明 |
                        |--------|------|
                        | 200 | 成功 |
                        | 400 | 参数错误 |
                        | 401 | 未认证/Token过期 |
                        | 403 | 无权限 |
                        | 429 | 请求过于频繁 |
                        | 500 | 服务器错误 |
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("技术支持")
                        .email("support@xindai.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }
}
