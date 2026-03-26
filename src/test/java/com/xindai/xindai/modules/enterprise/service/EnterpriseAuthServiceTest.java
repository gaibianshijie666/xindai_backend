package com.xindai.xindai.modules.enterprise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseLoginDTO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseUserVO;
import com.xindai.xindai.modules.enterprise.entity.Enterprise;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseUser;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseMapper;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseUserMapper;
import com.xindai.xindai.modules.enterprise.service.impl.EnterpriseAuthServiceImpl;
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
@DisplayName("EnterpriseAuthService 单元测试")
class EnterpriseAuthServiceTest {

    @Mock
    private EnterpriseUserMapper enterpriseUserMapper;

    @Mock
    private EnterpriseMapper enterpriseMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private EnterpriseAuthServiceImpl enterpriseAuthService;

    private Enterprise testEnterprise;
    private EnterpriseUser testUser;
    private EnterpriseLoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        testEnterprise = new Enterprise();
        testEnterprise.setId(1L);
        testEnterprise.setEnterpriseNo("ENT001");
        testEnterprise.setName("测试企业");
        testEnterprise.setStatus(0);

        testUser = new EnterpriseUser();
        testUser.setId(1L);
        testUser.setEnterpriseId(1L);
        testUser.setUsername("admin");
        testUser.setPasswordHash("hashed_password");
        testUser.setRealName("管理员");
        testUser.setPhone("13800138000");
        testUser.setRole(1);  // 1=ADMIN
        testUser.setStatus(0);

        loginDTO = new EnterpriseLoginDTO();
        loginDTO.setEnterpriseNo("ENT001");
        loginDTO.setUsername("admin");
        loginDTO.setPassword("password123");
    }

    @Nested
    @DisplayName("企业登录测试")
    class LoginTests {

        @Test
        @DisplayName("登录成功")
        void login_Success() {
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEnterprise);
            when(enterpriseUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtils.generateToken(anyLong(), anyString(), eq("ENTERPRISE"), anyLong())).thenReturn("enterprise_token");

            EnterpriseUserVO result = enterpriseAuthService.login(loginDTO);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(1L, result.getEnterpriseId());
            assertEquals("测试企业", result.getEnterpriseName());
            assertEquals("admin", result.getUsername());
            assertEquals("enterprise_token", result.getToken());
        }

        @Test
        @DisplayName("企业不存在 - 抛出异常")
        void login_EnterpriseNotFound_ThrowsException() {
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> enterpriseAuthService.login(loginDTO));

            assertEquals(ErrorCode.ENTERPRISE_NOT_FOUND.getCode(), exception.getCode());
            verify(enterpriseUserMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("企业已禁用 - 抛出异常")
        void login_EnterpriseDisabled_ThrowsException() {
            testEnterprise.setStatus(1);
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEnterprise);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> enterpriseAuthService.login(loginDTO));

            assertEquals(ErrorCode.ENTERPRISE_DISABLED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void login_UserNotFound_ThrowsException() {
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEnterprise);
            when(enterpriseUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> enterpriseAuthService.login(loginDTO));

            assertEquals(ErrorCode.ENTERPRISE_USER_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("用户已禁用 - 抛出异常")
        void login_UserDisabled_ThrowsException() {
            testUser.setStatus(1);
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEnterprise);
            when(enterpriseUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> enterpriseAuthService.login(loginDTO));

            assertEquals(ErrorCode.USER_DISABLED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("密码错误 - 抛出异常")
        void login_WrongPassword_ThrowsException() {
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEnterprise);
            when(enterpriseUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> enterpriseAuthService.login(loginDTO));

            assertEquals(ErrorCode.PASSWORD_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("Token生成验证")
        void login_TokenGeneration_Verified() {
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEnterprise);
            when(enterpriseUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtils.generateToken(1L, "admin", "ENTERPRISE", 1L)).thenReturn("generated_token");

            EnterpriseUserVO result = enterpriseAuthService.login(loginDTO);

            assertEquals("generated_token", result.getToken());
            verify(jwtUtils).generateToken(1L, "admin", "ENTERPRISE", 1L);
        }

        @Test
        @DisplayName("不同角色用户登录")
        void login_DifferentRoles() {
            testUser.setRole(2);  // 2=OPERATOR
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEnterprise);
            when(enterpriseUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtils.generateToken(anyLong(), anyString(), eq("ENTERPRISE"), anyLong())).thenReturn("token");

            EnterpriseUserVO result = enterpriseAuthService.login(loginDTO);

            assertNotNull(result);
            assertEquals(2, result.getRole());  // 2=OPERATOR
        }
    }

    @Nested
    @DisplayName("获取当前用户测试")
    class GetCurrentUserTests {

        @Test
        @DisplayName("获取当前用户 - 成功")
        void getCurrentUser_Success() {
            when(enterpriseUserMapper.selectById(1L)).thenReturn(testUser);

            EnterpriseUser result = enterpriseAuthService.getCurrentUser(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("admin", result.getUsername());
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void getCurrentUser_NotFound_ThrowsException() {
            when(enterpriseUserMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> enterpriseAuthService.getCurrentUser(999L));

            assertEquals(ErrorCode.ENTERPRISE_USER_NOT_FOUND.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("获取企业信息测试")
    class GetEnterpriseTests {

        @Test
        @DisplayName("获取企业信息 - 成功")
        void getEnterprise_Success() {
            when(enterpriseMapper.selectById(1L)).thenReturn(testEnterprise);

            Enterprise result = enterpriseAuthService.getEnterprise(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("测试企业", result.getName());
        }

        @Test
        @DisplayName("企业不存在 - 抛出异常")
        void getEnterprise_NotFound_ThrowsException() {
            when(enterpriseMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> enterpriseAuthService.getEnterprise(999L));

            assertEquals(ErrorCode.ENTERPRISE_NOT_FOUND.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("企业编号精确匹配")
        void login_EnterpriseNoExactMatch() {
            loginDTO.setEnterpriseNo("ENT002");
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThrows(BusinessException.class, () -> enterpriseAuthService.login(loginDTO));
        }

        @Test
        @DisplayName("用户名精确匹配")
        void login_UsernameExactMatch() {
            loginDTO.setUsername("different_user");
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEnterprise);
            when(enterpriseUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThrows(BusinessException.class, () -> enterpriseAuthService.login(loginDTO));
        }

        @Test
        @DisplayName("多个用户同企业登录")
        void login_MultipleUsersInSameEnterprise() {
            EnterpriseUser user2 = new EnterpriseUser();
            user2.setId(2L);
            user2.setEnterpriseId(1L);
            user2.setUsername("operator");
            user2.setPasswordHash("hashed_password");
            user2.setStatus(0);

            loginDTO.setUsername("operator");

            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEnterprise);
            when(enterpriseUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user2);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtils.generateToken(anyLong(), anyString(), eq("ENTERPRISE"), anyLong())).thenReturn("token");

            EnterpriseUserVO result = enterpriseAuthService.login(loginDTO);

            assertNotNull(result);
            assertEquals(2L, result.getId());
            assertEquals("operator", result.getUsername());
        }

        @Test
        @DisplayName("空企业编号处理")
        void login_EmptyEnterpriseNo() {
            loginDTO.setEnterpriseNo("");
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThrows(BusinessException.class, () -> enterpriseAuthService.login(loginDTO));
        }

        @Test
        @DisplayName("空用户名处理")
        void login_EmptyUsername() {
            loginDTO.setUsername("");
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEnterprise);
            when(enterpriseUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThrows(BusinessException.class, () -> enterpriseAuthService.login(loginDTO));
        }
    }

    @Nested
    @DisplayName("VO构建测试")
    class VoBuilderTests {

        @Test
        @DisplayName("VO包含完整用户信息")
        void login_VoContainsFullUserInfo() {
            testUser.setRealName("张三");
            testUser.setPhone("13900139000");
            testUser.setRole(1);  // 1=ADMIN

            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEnterprise);
            when(enterpriseUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtils.generateToken(anyLong(), anyString(), eq("ENTERPRISE"), anyLong())).thenReturn("token");

            EnterpriseUserVO result = enterpriseAuthService.login(loginDTO);

            assertEquals("张三", result.getRealName());
            assertEquals("13900139000", result.getPhone());
            assertEquals("ADMIN", result.getRole());
        }

        @Test
        @DisplayName("VO包含企业信息")
        void login_VoContainsEnterpriseInfo() {
            when(enterpriseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEnterprise);
            when(enterpriseUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtils.generateToken(anyLong(), anyString(), eq("ENTERPRISE"), anyLong())).thenReturn("token");

            EnterpriseUserVO result = enterpriseAuthService.login(loginDTO);

            assertEquals(1L, result.getEnterpriseId());
            assertEquals("测试企业", result.getEnterpriseName());
        }
    }
}
