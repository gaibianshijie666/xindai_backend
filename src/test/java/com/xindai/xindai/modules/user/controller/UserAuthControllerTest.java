package com.xindai.xindai.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.user.dto.UserLoginDTO;
import com.xindai.xindai.modules.user.dto.UserRegisterDTO;
import com.xindai.xindai.modules.user.dto.UserVO;
import com.xindai.xindai.modules.user.service.UserAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserAuthController.class)
@DisplayName("UserAuthController 单元测试")
class UserAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserAuthService userAuthService;

    private UserRegisterDTO registerDTO;
    private UserLoginDTO loginDTO;
    private UserVO userVO;

    @BeforeEach
    void setUp() {
        registerDTO = new UserRegisterDTO();
        registerDTO.setPhone("13800138000");
        registerDTO.setPassword("password123");

        loginDTO = new UserLoginDTO();
        loginDTO.setPhone("13800138000");
        loginDTO.setPassword("password123");

        userVO = new UserVO();
        userVO.setId(1L);
        userVO.setPhone("13800138000");
        userVO.setToken("test_token");
    }

    @Nested
    @DisplayName("注册接口测试")
    class RegisterTests {

        @Test
        @DisplayName("注册成功 - 返回200")
        void register_Success() throws Exception {
            when(userAuthService.register(any(UserRegisterDTO.class))).thenReturn(userVO);

            mockMvc.perform(post("/api/v1/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.phone").value("13800138000"))
                    .andExpect(jsonPath("$.data.token").exists());
        }

        @Test
        @DisplayName("手机号已存在 - 返回错误")
        void register_PhoneExists() throws Exception {
            when(userAuthService.register(any(UserRegisterDTO.class)))
                    .thenThrow(new BusinessException(ErrorCode.PHONE_EXISTS));

            mockMvc.perform(post("/api/v1/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(jsonPath("$.code").value(ErrorCode.PHONE_EXISTS.getCode()));
        }

        @Test
        @DisplayName("手机号为空 - 验证失败")
        void register_EmptyPhone() throws Exception {
            registerDTO.setPhone("");

            mockMvc.perform(post("/api/v1/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("手机号格式错误 - 验证失败")
        void register_InvalidPhone() throws Exception {
            registerDTO.setPhone("12345");

            mockMvc.perform(post("/api/v1/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("密码为空 - 验证失败")
        void register_EmptyPassword() throws Exception {
            registerDTO.setPassword("");

            mockMvc.perform(post("/api/v1/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("密码过短 - 验证失败")
        void register_TooShortPassword() throws Exception {
            registerDTO.setPassword("12345");

            mockMvc.perform(post("/api/v1/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("登录接口测试")
    class LoginTests {

        @Test
        @DisplayName("登录成功 - 返回Token")
        void login_Success() throws Exception {
            when(userAuthService.login(any(UserLoginDTO.class))).thenReturn(userVO);

            mockMvc.perform(post("/api/v1/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.token").value("test_token"));
        }

        @Test
        @DisplayName("用户不存在 - 返回错误")
        void login_UserNotFound() throws Exception {
            when(userAuthService.login(any(UserLoginDTO.class)))
                    .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(post("/api/v1/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("密码错误 - 返回错误")
        void login_WrongPassword() throws Exception {
            when(userAuthService.login(any(UserLoginDTO.class)))
                    .thenThrow(new BusinessException(ErrorCode.PASSWORD_ERROR));

            mockMvc.perform(post("/api/v1/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(jsonPath("$.code").value(ErrorCode.PASSWORD_ERROR.getCode()));
        }

        @Test
        @DisplayName("用户已禁用 - 返回错误")
        void login_UserDisabled() throws Exception {
            when(userAuthService.login(any(UserLoginDTO.class)))
                    .thenThrow(new BusinessException(ErrorCode.USER_DISABLED));

            mockMvc.perform(post("/api/v1/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(jsonPath("$.code").value(ErrorCode.USER_DISABLED.getCode()));
        }

        @Test
        @DisplayName("手机号为空 - 验证失败")
        void login_EmptyPhone() throws Exception {
            loginDTO.setPhone("");

            mockMvc.perform(post("/api/v1/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("密码为空 - 验证失败")
        void login_EmptyPassword() throws Exception {
            loginDTO.setPassword("");

            mockMvc.perform(post("/api/v1/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isBadRequest());
        }
    }
}
