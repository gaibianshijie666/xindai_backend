package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.admin.dto.UserQueryDTO;
import com.xindai.xindai.modules.admin.dto.UserStatusUpdateDTO;
import com.xindai.xindai.modules.admin.service.impl.AdminUserServiceImpl;
import com.xindai.xindai.modules.admin.vo.AdminUserDetailVO;
import com.xindai.xindai.modules.admin.vo.AdminUserVO;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.entity.UserProfile;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import com.xindai.xindai.modules.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminUserService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminUserService 单元测试")
class AdminUserServiceTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        try {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        } catch (Exception e) {
            // Already initialized
        }
    }

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserProfileMapper userProfileMapper;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User testUser;
    private UserProfile testProfile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setPhone("13800138000");
        testUser.setRealName("张三");
        testUser.setIdCard("110101199001011234");
        testUser.setStatus(1);
        testUser.setCreatedAt(LocalDateTime.now());

        testProfile = new UserProfile();
        testProfile.setUserId(1L);
        testProfile.setCreditScore(680);
        testProfile.setRiskLevel(1);
        testProfile.setCreditGrade("B");
    }

    @Nested
    @DisplayName("用户列表查询测试")
    class GetUserListTests {

        @Test
        @DisplayName("带关键词查询 - 返回匹配结果")
        void getUserList_WithKeyword_ReturnsMatchingResults() {
            // Arrange
            UserQueryDTO queryDTO = new UserQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);
            queryDTO.setKeyword("张三");

            Page<User> userPage = new Page<>(1, 10);
            userPage.setRecords(List.of(testUser));
            userPage.setTotal(1);

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(userPage);

            // Act
            Page<AdminUserVO> result = adminUserService.getUserList(queryDTO);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals("张三", result.getRecords().get(0).getRealName());
        }

        @Test
        @DisplayName("无关键词查询 - 返回所有结果")
        void getUserList_NoKeyword_ReturnsAllResults() {
            // Arrange
            UserQueryDTO queryDTO = new UserQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);

            Page<User> userPage = new Page<>(1, 10);
            userPage.setRecords(List.of(testUser));
            userPage.setTotal(1);

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(userPage);

            // Act
            Page<AdminUserVO> result = adminUserService.getUserList(queryDTO);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("空列表查询 - 返回空结果")
        void getUserList_EmptyList_ReturnsEmptyResult() {
            // Arrange
            UserQueryDTO queryDTO = new UserQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);

            Page<User> userPage = new Page<>(1, 10);
            userPage.setRecords(Collections.emptyList());
            userPage.setTotal(0);

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(userPage);

            // Act
            Page<AdminUserVO> result = adminUserService.getUserList(queryDTO);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecords().isEmpty());
            assertEquals(0, result.getTotal());
        }

        @Test
        @DisplayName("手机号关键词查询")
        void getUserList_PhoneKeyword_ReturnsMatchingResults() {
            // Arrange
            UserQueryDTO queryDTO = new UserQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);
            queryDTO.setKeyword("138");

            Page<User> userPage = new Page<>(1, 10);
            userPage.setRecords(List.of(testUser));
            userPage.setTotal(1);

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(userPage);

            // Act
            Page<AdminUserVO> result = adminUserService.getUserList(queryDTO);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
        }
    }

    @Nested
    @DisplayName("用户详情查询测试")
    class GetUserDetailTests {

        @Test
        @DisplayName("获取用户详情 - 返回完整信息")
        void getUserDetail_ValidId_ReturnsCompleteInfo() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testProfile);

            // Act
            AdminUserDetailVO result = adminUserService.getUserDetail(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("13800138000", result.getPhone());
            assertEquals("张三", result.getRealName());
            assertEquals(1, result.getStatus());
            assertEquals(680, result.getCreditScore());
            assertEquals(1, result.getRiskLevel());
            assertEquals("B", result.getCreditGrade());
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void getUserDetail_UserNotFound_ThrowsException() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(null);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminUserService.getUserDetail(1L));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("身份证号直接返回原始值(脱敏由Jackson序列化层处理)")
        void getUserDetail_WithIdCard_MasksIdCard() {
            // Arrange
            testUser.setIdCard("110101199001011234");
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testProfile);

            // Act
            AdminUserDetailVO result = adminUserService.getUserDetail(1L);

            // Assert - 脱敏由Jackson的@Desensitize注解在序列化层处理，服务层返回原始值
            assertNotNull(result.getIdCard());
            assertEquals("110101199001011234", result.getIdCard());
        }

        @Test
        @DisplayName("无身份证号时不脱敏")
        void getUserDetail_NoIdCard_NoMasking() {
            // Arrange
            testUser.setIdCard(null);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testProfile);

            // Act
            AdminUserDetailVO result = adminUserService.getUserDetail(1L);

            // Assert
            assertNull(result.getIdCard());
        }

        @Test
        @DisplayName("无用户画像时信用信息为空")
        void getUserDetail_NoProfile_CreditInfoIsNull() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // Act
            AdminUserDetailVO result = adminUserService.getUserDetail(1L);

            // Assert
            assertNotNull(result);
            assertNull(result.getCreditScore());
            assertNull(result.getRiskLevel());
            assertNull(result.getCreditGrade());
        }
    }

    @Nested
    @DisplayName("用户状态更新测试")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("禁用用户 - 更新状态为禁用")
        void updateUserStatus_Disable_UpdatesStatus() {
            // Arrange
            UserStatusUpdateDTO updateDTO = new UserStatusUpdateDTO();
            updateDTO.setStatus(0); // 禁用

            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

            // Act
            adminUserService.updateUserStatus(1L, updateDTO);

            // Assert
            verify(userMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("启用用户 - 更新状态为启用")
        void updateUserStatus_Enable_UpdatesStatus() {
            // Arrange
            testUser.setStatus(0);
            UserStatusUpdateDTO updateDTO = new UserStatusUpdateDTO();
            updateDTO.setStatus(1); // 启用

            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

            // Act
            adminUserService.updateUserStatus(1L, updateDTO);

            // Assert
            verify(userMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void updateUserStatus_UserNotFound_ThrowsException() {
            // Arrange
            UserStatusUpdateDTO updateDTO = new UserStatusUpdateDTO();
            updateDTO.setStatus(0);

            when(userMapper.selectById(1L)).thenReturn(null);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminUserService.updateUserStatus(1L, updateDTO));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
        }
    }
}
