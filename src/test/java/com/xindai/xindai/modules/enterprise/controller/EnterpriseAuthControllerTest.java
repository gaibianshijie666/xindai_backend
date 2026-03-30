package com.xindai.xindai.modules.enterprise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseLoginDTO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseUserVO;
import com.xindai.xindai.modules.enterprise.entity.Enterprise;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseUser;
import com.xindai.xindai.modules.enterprise.service.EnterpriseAuthService;
import com.xindai.xindai.security.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mockito.Answers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnterpriseAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EnterpriseAuthController 单元测试")
class EnterpriseAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnterpriseAuthService enterpriseAuthService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private SqlSessionFactory sqlSessionFactory;

    private EnterpriseLoginDTO loginDTO;
    private EnterpriseUserVO userVO;
    private Enterprise testEnterprise;
    private EnterpriseUser testUser;

    @BeforeEach
    void setUp() {
        loginDTO = new EnterpriseLoginDTO();
        loginDTO.setEnterpriseNo("ENT001");
        loginDTO.setUsername("admin");
        loginDTO.setPassword("password123");

        userVO = new EnterpriseUserVO();
        userVO.setId(1L);
        userVO.setEnterpriseId(1L);
        userVO.setEnterpriseName("测试企业");
        userVO.setUsername("admin");
        userVO.setRealName("管理员");
        userVO.setPhone("13800138000");
        userVO.setRole(1);  // 1=ADMIN
        userVO.setToken("enterprise_token");

        testEnterprise = new Enterprise();
        testEnterprise.setId(1L);
        testEnterprise.setEnterpriseNo("ENT001");
        testEnterprise.setName("测试企业");

        testUser = new EnterpriseUser();
        testUser.setId(1L);
        testUser.setEnterpriseId(1L);
        testUser.setUsername("admin");
        testUser.setRealName("管理员");
    }

    @Nested
    @DisplayName("企业登录接口测试")
    class LoginTests {

        @Test
        @DisplayName("登录成功 - 返回Token和用户信息")
        void login_Success() throws Exception {
            when(enterpriseAuthService.login(any(EnterpriseLoginDTO.class))).thenReturn(userVO);

            mockMvc.perform(post("/api/v1/enterprise/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.token").value("enterprise_token"))
                    .andExpect(jsonPath("$.data.enterpriseName").value("测试企业"))
                    .andExpect(jsonPath("$.data.username").value("admin"));
        }

        @Test
        @DisplayName("企业不存在 - 返回错误")
        void login_EnterpriseNotFound() throws Exception {
            when(enterpriseAuthService.login(any(EnterpriseLoginDTO.class)))
                    .thenThrow(new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND));

            mockMvc.perform(post("/api/v1/enterprise/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(jsonPath("$.code").value(ErrorCode.ENTERPRISE_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("企业已禁用 - 返回错误")
        void login_EnterpriseDisabled() throws Exception {
            when(enterpriseAuthService.login(any(EnterpriseLoginDTO.class)))
                    .thenThrow(new BusinessException(ErrorCode.ENTERPRISE_DISABLED));

            mockMvc.perform(post("/api/v1/enterprise/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(jsonPath("$.code").value(ErrorCode.ENTERPRISE_DISABLED.getCode()));
        }

        @Test
        @DisplayName("用户不存在 - 返回错误")
        void login_UserNotFound() throws Exception {
            when(enterpriseAuthService.login(any(EnterpriseLoginDTO.class)))
                    .thenThrow(new BusinessException(ErrorCode.ENTERPRISE_USER_NOT_FOUND));

            mockMvc.perform(post("/api/v1/enterprise/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(jsonPath("$.code").value(ErrorCode.ENTERPRISE_USER_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("密码错误 - 返回错误")
        void login_WrongPassword() throws Exception {
            when(enterpriseAuthService.login(any(EnterpriseLoginDTO.class)))
                    .thenThrow(new BusinessException(ErrorCode.PASSWORD_ERROR));

            mockMvc.perform(post("/api/v1/enterprise/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(jsonPath("$.code").value(ErrorCode.PASSWORD_ERROR.getCode()));
        }

        @Test
        @DisplayName("用户已禁用 - 返回错误")
        void login_UserDisabled() throws Exception {
            when(enterpriseAuthService.login(any(EnterpriseLoginDTO.class)))
                    .thenThrow(new BusinessException(ErrorCode.USER_DISABLED));

            mockMvc.perform(post("/api/v1/enterprise/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(jsonPath("$.code").value(ErrorCode.USER_DISABLED.getCode()));
        }

        @Test
        @DisplayName("企业编号为空 - 验证失败")
        void login_EmptyEnterpriseNo() throws Exception {
            loginDTO.setEnterpriseNo(null);

            mockMvc.perform(post("/api/v1/enterprise/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("用户名为空 - 验证失败")
        void login_EmptyUsername() throws Exception {
            loginDTO.setUsername("");

            mockMvc.perform(post("/api/v1/enterprise/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("密码为空 - 验证失败")
        void login_EmptyPassword() throws Exception {
            loginDTO.setPassword("");

            mockMvc.perform(post("/api/v1/enterprise/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("获取用户信息接口测试")
    class ProfileTests {

        @Test
        @DisplayName("获取用户信息 - 无认证时返回成功但数据为null")
        void profile_NoAuth() throws Exception {
            mockMvc.perform(get("/api/v1/enterprise/auth/profile"))
                    .andExpect(status().isOk());
        }
    }
}
