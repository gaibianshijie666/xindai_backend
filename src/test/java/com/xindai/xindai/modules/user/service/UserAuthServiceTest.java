package com.xindai.xindai.modules.user.service;

import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.user.dto.UserLoginDTO;
import com.xindai.xindai.modules.user.dto.UserRegisterDTO;
import com.xindai.xindai.modules.user.dto.UserVO;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import com.xindai.xindai.modules.user.service.impl.UserAuthServiceImpl;
import com.xindai.xindai.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuthService 单元测试")
class UserAuthServiceTest {

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserAuthServiceImpl userAuthService;

    private User testUser;
    private UserRegisterDTO registerDTO;
    private UserLoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setPhone("13800138000");
        testUser.setPasswordHash("hashed_password");
        testUser.setStatus(1);

        registerDTO = new UserRegisterDTO();
        registerDTO.setPhone("13800138000");
        registerDTO.setPassword("password123");

        loginDTO = new UserLoginDTO();
        loginDTO.setPhone("13800138000");
        loginDTO.setPassword("password123");
    }

    @Nested
    @DisplayName("注册功能测试")
    class RegisterTests {

        @Test
        @DisplayName("注册成功")
        void register_Success() {
            when(userProfileService.getByPhone(anyString())).thenReturn(null);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
            when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return 1;
            });
            when(jwtUtils.generateToken(any(), anyString())).thenReturn("test_token");

            UserVO result = userAuthService.register(registerDTO);

            assertNotNull(result);
            assertEquals("13800138000", result.getPhone());
            assertEquals("test_token", result.getToken());
            verify(userMapper, times(1)).insert(any(User.class));
        }

        @Test
        @DisplayName("手机号已存在 - 抛出异常")
        void register_PhoneExists_ThrowsException() {
            when(userProfileService.getByPhone(anyString())).thenReturn(testUser);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userAuthService.register(registerDTO));

            assertEquals(ErrorCode.PHONE_EXISTS.getCode(), exception.getCode());
            verify(userMapper, never()).insert(any(User.class));
        }

        @Test
        @DisplayName("密码加密调用验证")
        void register_PasswordEncoding_Called() {
            when(userProfileService.getByPhone(anyString())).thenReturn(null);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
            when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
                inv.getArgument(0, User.class).setId(1L);
                return 1;
            });
            when(jwtUtils.generateToken(any(), anyString())).thenReturn("token");

            userAuthService.register(registerDTO);

            verify(passwordEncoder, times(1)).encode("password123");
        }
    }

    @Nested
    @DisplayName("登录功能测试")
    class LoginTests {

        @Test
        @DisplayName("登录成功")
        void login_Success() {
            when(userProfileService.getByPhone(anyString())).thenReturn(testUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtils.generateToken(any(), anyString())).thenReturn("test_token");

            UserVO result = userAuthService.login(loginDTO);

            assertNotNull(result);
            assertEquals("test_token", result.getToken());
            assertEquals("13800138000", result.getPhone());
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void login_UserNotFound_ThrowsException() {
            when(userProfileService.getByPhone(anyString())).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userAuthService.login(loginDTO));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("密码错误 - 抛出异常")
        void login_WrongPassword_ThrowsException() {
            when(userProfileService.getByPhone(anyString())).thenReturn(testUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userAuthService.login(loginDTO));

            assertEquals(ErrorCode.PASSWORD_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("用户已禁用 - 抛出异常")
        void login_UserDisabled_ThrowsException() {
            testUser.setStatus(0);
            when(userProfileService.getByPhone(anyString())).thenReturn(testUser);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userAuthService.login(loginDTO));

            assertEquals(ErrorCode.USER_DISABLED.getCode(), exception.getCode());
        }
    }
}
