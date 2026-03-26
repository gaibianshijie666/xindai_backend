package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.admin.dto.BlacklistAddDTO;
import com.xindai.xindai.modules.admin.dto.BlacklistQueryDTO;
import com.xindai.xindai.modules.admin.service.impl.AdminBlacklistServiceImpl;
import com.xindai.xindai.modules.admin.vo.BlacklistVO;
import com.xindai.xindai.modules.risk.entity.Blacklist;
import com.xindai.xindai.modules.risk.mapper.BlacklistMapper;
import com.xindai.xindai.modules.risk.service.BlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminBlacklistService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminBlacklistService 单元测试")
class AdminBlacklistServiceTest {

    @Mock
    private BlacklistMapper blacklistMapper;

    @Mock
    private BlacklistService blacklistService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private AdminBlacklistServiceImpl adminBlacklistService;

    private Blacklist testBlacklist;
    private BlacklistAddDTO testAddDTO;

    @BeforeEach
    void setUp() {
        testBlacklist = new Blacklist();
        testBlacklist.setId(1L);
        testBlacklist.setType(1);
        testBlacklist.setValue("13800138000");
        testBlacklist.setReason("欺诈行为");
        testBlacklist.setCreatedAt(LocalDateTime.now());

        testAddDTO = new BlacklistAddDTO();
        testAddDTO.setType(1);
        testAddDTO.setValue("13800138000");
        testAddDTO.setReason("欺诈行为");
    }

    @Nested
    @DisplayName("添加黑名单测试")
    class AddBlacklistTests {

        @Test
        @DisplayName("添加手机号黑名单 - 成功")
        void addToBlacklist_Phone_AddsEntry() {
            // Arrange
            when(blacklistMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(blacklistMapper.insert(any(Blacklist.class))).thenReturn(1);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            // Act
            adminBlacklistService.addBlacklist(testAddDTO);

            // Assert
            verify(blacklistMapper).insert(any(Blacklist.class));
            verify(setOperations).add("blacklist:phone", "13800138000");
        }

        @Test
        @DisplayName("添加身份证黑名单 - 成功")
        void addToBlacklist_IdCard_AddsEntry() {
            // Arrange
            testAddDTO.setType(2);
            testAddDTO.setValue("110101199001011234");

            when(blacklistMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(blacklistMapper.insert(any(Blacklist.class))).thenReturn(1);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            // Act
            adminBlacklistService.addBlacklist(testAddDTO);

            // Assert
            verify(blacklistMapper).insert(any(Blacklist.class));
            verify(setOperations).add("blacklist:idcard", "110101199001011234");
        }

        @Test
        @DisplayName("添加设备ID黑名单 - 成功")
        void addToBlacklist_DeviceId_AddsEntry() {
            // Arrange
            testAddDTO.setType(3);
            testAddDTO.setValue("device123456");

            when(blacklistMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(blacklistMapper.insert(any(Blacklist.class))).thenReturn(1);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            // Act
            adminBlacklistService.addBlacklist(testAddDTO);

            // Assert
            verify(blacklistMapper).insert(any(Blacklist.class));
            verify(setOperations).add("blacklist:device", "device123456");
        }

        @Test
        @DisplayName("黑名单已存在 - 抛出异常")
        void addToBlacklist_AlreadyExists_ThrowsException() {
            // Arrange
            when(blacklistMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminBlacklistService.addBlacklist(testAddDTO));

            assertEquals(ErrorCode.BLACKLIST_ALREADY_EXISTS.getCode(), exception.getCode());
            verify(blacklistMapper, never()).insert(any(Blacklist.class));
        }

        @ParameterizedTest
        @CsvSource({
                "1, 13800138000, 手机号黑名单",
                "2, 110101199001011234, 身份证黑名单",
                "3, device123, 设备黑名单"
        })
        @DisplayName("不同类型黑名单添加")
        void addToBlacklist_DifferentTypes(int type, String value, String description) {
            // Arrange
            testAddDTO.setType(type);
            testAddDTO.setValue(value);

            when(blacklistMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(blacklistMapper.insert(any(Blacklist.class))).thenReturn(1);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            // Act
            adminBlacklistService.addBlacklist(testAddDTO);

            // Assert
            verify(blacklistMapper).insert(any(Blacklist.class));
        }
    }

    @Nested
    @DisplayName("移除黑名单测试")
    class RemoveBlacklistTests {

        @Test
        @DisplayName("移除已存在黑名单 - 成功")
        void removeFromBlacklist_ExistingEntry_RemovesEntry() {
            // Arrange
            when(blacklistMapper.selectById(1L)).thenReturn(testBlacklist);
            when(blacklistMapper.deleteById(1L)).thenReturn(1);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            // Act
            adminBlacklistService.removeBlacklist(1L);

            // Assert
            verify(blacklistMapper).deleteById(1L);
            verify(setOperations).remove("blacklist:phone", "13800138000");
        }

        @Test
        @DisplayName("移除身份证黑名单 - 成功")
        void removeFromBlacklist_IdCardType_RemovesEntry() {
            // Arrange
            testBlacklist.setType(2);
            testBlacklist.setValue("110101199001011234");

            when(blacklistMapper.selectById(1L)).thenReturn(testBlacklist);
            when(blacklistMapper.deleteById(1L)).thenReturn(1);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            // Act
            adminBlacklistService.removeBlacklist(1L);

            // Assert
            verify(blacklistMapper).deleteById(1L);
            verify(setOperations).remove("blacklist:idcard", "110101199001011234");
        }

        @Test
        @DisplayName("黑名单不存在 - 抛出异常")
        void removeFromBlacklist_NotFound_ThrowsException() {
            // Arrange
            when(blacklistMapper.selectById(1L)).thenReturn(null);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminBlacklistService.removeBlacklist(1L));

            assertEquals(ErrorCode.BLACKLIST_NOT_FOUND.getCode(), exception.getCode());
            verify(blacklistMapper, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("查询黑名单测试")
    class QueryBlacklistTests {

        @Test
        @DisplayName("带类型过滤查询 - 返回分页结果")
        void queryBlacklist_WithFilters_ReturnsPagedResult() {
            // Arrange
            BlacklistQueryDTO queryDTO = new BlacklistQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);
            queryDTO.setType(1);

            Page<Blacklist> blacklistPage = new Page<>(1, 10);
            blacklistPage.setRecords(List.of(testBlacklist));
            blacklistPage.setTotal(1);

            when(blacklistMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(blacklistPage);

            // Act
            Page<BlacklistVO> result = adminBlacklistService.getBlacklistList(queryDTO);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(1, result.getTotal());
            BlacklistVO vo = result.getRecords().get(0);
            assertEquals(1, vo.getType());
            assertEquals("13800138000", vo.getValue());
            assertEquals("欺诈行为", vo.getReason());
        }

        @Test
        @DisplayName("无类型过滤查询 - 返回所有结果")
        void queryBlacklist_NoFilters_ReturnsAllResults() {
            // Arrange
            BlacklistQueryDTO queryDTO = new BlacklistQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);
            queryDTO.setType(null);

            Page<Blacklist> blacklistPage = new Page<>(1, 10);
            blacklistPage.setRecords(List.of(testBlacklist));
            blacklistPage.setTotal(1);

            when(blacklistMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(blacklistPage);

            // Act
            Page<BlacklistVO> result = adminBlacklistService.getBlacklistList(queryDTO);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("空列表查询 - 返回空结果")
        void queryBlacklist_EmptyList_ReturnsEmptyResult() {
            // Arrange
            BlacklistQueryDTO queryDTO = new BlacklistQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);

            Page<Blacklist> blacklistPage = new Page<>(1, 10);
            blacklistPage.setRecords(Collections.emptyList());
            blacklistPage.setTotal(0);

            when(blacklistMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(blacklistPage);

            // Act
            Page<BlacklistVO> result = adminBlacklistService.getBlacklistList(queryDTO);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecords().isEmpty());
            assertEquals(0, result.getTotal());
        }

        @Test
        @DisplayName("分页查询 - 正确分页")
        void queryBlacklist_Pagination_ReturnsCorrectPage() {
            // Arrange
            BlacklistQueryDTO queryDTO = new BlacklistQueryDTO();
            queryDTO.setPage(2);
            queryDTO.setSize(5);

            Blacklist blacklist1 = new Blacklist();
            blacklist1.setId(6L);
            blacklist1.setType(1);
            blacklist1.setValue("phone6");

            Blacklist blacklist2 = new Blacklist();
            blacklist2.setId(7L);
            blacklist2.setType(1);
            blacklist2.setValue("phone7");

            Page<Blacklist> blacklistPage = new Page<>(2, 5);
            blacklistPage.setRecords(List.of(blacklist1, blacklist2));
            blacklistPage.setTotal(12);

            when(blacklistMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(blacklistPage);

            // Act
            Page<BlacklistVO> result = adminBlacklistService.getBlacklistList(queryDTO);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.getRecords().size());
            assertEquals(12, result.getTotal());
            assertEquals(2, result.getCurrent());
        }
    }
}
