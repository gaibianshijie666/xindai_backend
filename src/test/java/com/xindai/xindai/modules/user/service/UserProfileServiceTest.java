package com.xindai.xindai.modules.user.service;

import com.xindai.xindai.client.kyc.KycResult;
import com.xindai.xindai.client.kyc.KycService;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.user.dto.PasswordChangeDTO;
import com.xindai.xindai.modules.user.dto.UserUpdateDTO;
import com.xindai.xindai.modules.user.dto.UserVO;
import com.xindai.xindai.modules.user.dto.VerifyIdentityDTO;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.entity.UserProfile;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import com.xindai.xindai.modules.user.mapper.UserProfileMapper;
import com.xindai.xindai.modules.user.service.impl.UserProfileServiceImpl;
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
@DisplayName("UserProfileService 单元测试")
class UserProfileServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private KycService kycService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setPhone("13800138000");
        testUser.setPasswordHash("hashed_password");
        testUser.setStatus(1);
    }

    @Nested
    @DisplayName("更新资料测试")
    class UpdateProfileTests {

        @Test
        @DisplayName("更新资料成功")
        void updateProfile_Success() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setRealName("张三");
            dto.setIdCard("110101199001011234");

            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            UserVO result = userProfileService.updateProfile(1L, dto);

            assertNotNull(result);
            assertEquals("张三", result.getRealName());
            assertEquals("110101199001011234", result.getIdCard());
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void updateProfile_UserNotFound_ThrowsException() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> userProfileService.updateProfile(999L, new UserUpdateDTO()));
        }
    }

    @Nested
    @DisplayName("修改密码测试")
    class ChangePasswordTests {

        @Test
        @DisplayName("修改密码成功")
        void changePassword_Success() {
            PasswordChangeDTO dto = new PasswordChangeDTO();
            dto.setOldPassword("oldPassword");
            dto.setNewPassword("newPassword123");

            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(passwordEncoder.encode(anyString())).thenReturn("new_hashed");
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            assertDoesNotThrow(() -> userProfileService.changePassword(1L, dto));
            verify(jwtUtils).invalidateToken(1L);
        }

        @Test
        @DisplayName("旧密码错误 - 抛出异常")
        void changePassword_WrongOldPassword_ThrowsException() {
            PasswordChangeDTO dto = new PasswordChangeDTO();
            dto.setOldPassword("wrongPassword");
            dto.setNewPassword("newPassword123");

            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userProfileService.changePassword(1L, dto));

            assertEquals(ErrorCode.PASSWORD_ERROR.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("实名认证测试")
    class VerifyIdentityTests {

        @Test
        @DisplayName("实名认证成功")
        void verifyIdentity_Success() {
            VerifyIdentityDTO dto = new VerifyIdentityDTO();
            dto.setRealName("张三");
            dto.setIdCard("110101199001011234");

            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            KycResult kycResult = new KycResult();
            kycResult.setSuccess(true);
            when(kycService.verify("张三", "110101199001011234")).thenReturn(kycResult);

            UserVO result = userProfileService.verifyIdentity(1L, dto);

            assertNotNull(result);
            assertEquals("张三", result.getRealName());
        }

        @Test
        @DisplayName("已实名认证 - 抛出异常")
        void verifyIdentity_AlreadyVerified_ThrowsException() {
            testUser.setRealName("张三");
            testUser.setIdCard("110101199001011234");

            VerifyIdentityDTO dto = new VerifyIdentityDTO();
            dto.setRealName("李四");
            dto.setIdCard("110101199001015678");

            when(userMapper.selectById(1L)).thenReturn(testUser);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userProfileService.verifyIdentity(1L, dto));

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
        }
    }
}
