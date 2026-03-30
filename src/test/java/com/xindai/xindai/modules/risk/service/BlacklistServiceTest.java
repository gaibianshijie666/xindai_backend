package com.xindai.xindai.modules.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.modules.risk.entity.Blacklist;
import com.xindai.xindai.modules.risk.mapper.BlacklistMapper;
import com.xindai.xindai.modules.risk.service.impl.BlacklistServiceImpl;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlacklistService 单元测试")
class BlacklistServiceTest {

    @Mock
    private BlacklistMapper blacklistMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private BlacklistServiceImpl blacklistService;

    private Blacklist phoneBlacklist;
    private Blacklist idCardBlacklist;

    @BeforeEach
    void setUp() {
        phoneBlacklist = new Blacklist();
        phoneBlacklist.setId(1L);
        phoneBlacklist.setType(1);
        phoneBlacklist.setValue("13800138000");
        phoneBlacklist.setReason("欺诈行为");
        phoneBlacklist.setCreatedAt(LocalDateTime.now());

        idCardBlacklist = new Blacklist();
        idCardBlacklist.setId(2L);
        idCardBlacklist.setType(2);
        idCardBlacklist.setValue("110101199001011234");
        idCardBlacklist.setReason("恶意逃废债");
        idCardBlacklist.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("手机号黑名单检测测试")
    class PhoneBlacklistTests {

        @Test
        @DisplayName("手机号在缓存中 - 直接返回true")
        void isPhoneBlacklisted_InCache_ReturnsTrue() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember("blacklist:phone", "13800138000")).thenReturn(true);

            boolean result = blacklistService.isPhoneBlacklisted("13800138000");

            assertTrue(result);
            verify(blacklistMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("手机号不在缓存但在数据库中 - 返回true并加入缓存")
        void isPhoneBlacklisted_InDatabase_ReturnsTrueAndCacheIt() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember("blacklist:phone", "13800138000")).thenReturn(false);
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(phoneBlacklist);

            boolean result = blacklistService.isPhoneBlacklisted("13800138000");

            assertTrue(result);
            verify(setOperations).add("blacklist:phone", "13800138000");
        }

        @Test
        @DisplayName("手机号不在黑名单中 - 返回false")
        void isPhoneBlacklisted_NotInBlacklist_ReturnsFalse() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember("blacklist:phone", "13800138000")).thenReturn(false);
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            boolean result = blacklistService.isPhoneBlacklisted("13800138000");

            assertFalse(result);
            verify(setOperations, never()).add(anyString(), any());
        }

        @Test
        @DisplayName("黑名单记录已过期 - 返回false")
        void isPhoneBlacklist_Expired_ReturnsFalse() {
            phoneBlacklist.setExpireAt(LocalDateTime.now().minusDays(1));

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember("blacklist:phone", "13800138000")).thenReturn(false);
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            boolean result = blacklistService.isPhoneBlacklisted("13800138000");

            assertFalse(result);
        }

        @Test
        @DisplayName("黑名单记录未过期 - 返回true")
        void isPhoneBlacklist_NotExpired_ReturnsTrue() {
            phoneBlacklist.setExpireAt(LocalDateTime.now().plusDays(30));

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember("blacklist:phone", "13800138000")).thenReturn(false);
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(phoneBlacklist);

            boolean result = blacklistService.isPhoneBlacklisted("13800138000");

            assertTrue(result);
        }

        @Test
        @DisplayName("缓存返回null时查询数据库")
        void isPhoneBlacklisted_CacheNull_CheckDatabase() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember("blacklist:phone", "13800138000")).thenReturn(null);
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            boolean result = blacklistService.isPhoneBlacklisted("13800138000");

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("身份证黑名单检测测试")
    class IdCardBlacklistTests {

        @Test
        @DisplayName("身份证在缓存中 - 直接返回true")
        void isIdCardBlacklisted_InCache_ReturnsTrue() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember("blacklist:idcard", "110101199001011234")).thenReturn(true);

            boolean result = blacklistService.isIdCardBlacklisted("110101199001011234");

            assertTrue(result);
            verify(blacklistMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("身份证不在缓存但在数据库中 - 返回true并加入缓存")
        void isIdCardBlacklisted_InDatabase_ReturnsTrueAndCacheIt() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember("blacklist:idcard", "110101199001011234")).thenReturn(false);
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(idCardBlacklist);

            boolean result = blacklistService.isIdCardBlacklisted("110101199001011234");

            assertTrue(result);
            verify(setOperations).add("blacklist:idcard", "110101199001011234");
        }

        @Test
        @DisplayName("身份证不在黑名单中 - 返回false")
        void isIdCardBlacklisted_NotInBlacklist_ReturnsFalse() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember("blacklist:idcard", "110101199001011234")).thenReturn(false);
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            boolean result = blacklistService.isIdCardBlacklisted("110101199001011234");

            assertFalse(result);
        }

        @Test
        @DisplayName("身份证黑名单记录已过期 - 返回false")
        void isIdCardBlacklist_Expired_ReturnsFalse() {
            idCardBlacklist.setExpireAt(LocalDateTime.now().minusDays(1));

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember("blacklist:idcard", "110101199001011234")).thenReturn(false);
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            boolean result = blacklistService.isIdCardBlacklisted("110101199001011234");

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("添加黑名单测试")
    class AddBlacklistTests {

        @Test
        @DisplayName("添加手机号到黑名单")
        void addToBlacklist_Phone_Success() {
            when(blacklistMapper.insert(any(Blacklist.class))).thenReturn(1);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            blacklistService.addToBlacklist(1, "13800138000", "欺诈行为");

            verify(blacklistMapper).insert(any(Blacklist.class));
            verify(setOperations).add("blacklist:phone", "13800138000");
        }

        @Test
        @DisplayName("添加身份证到黑名单")
        void addToBlacklist_IdCard_Success() {
            when(blacklistMapper.insert(any(Blacklist.class))).thenReturn(1);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            blacklistService.addToBlacklist(2, "110101199001011234", "恶意逃废债");

            verify(blacklistMapper).insert(any(Blacklist.class));
            verify(setOperations).add("blacklist:idcard", "110101199001011234");
        }

        @ParameterizedTest
        @CsvSource({
                "1, 13800138000, 欺诈行为",
                "1, 15912345678, 恶意欠款",
                "2, 110101199001011234, 信用不良",
                "2, 320102198812129876, 逾期未还"
        })
        @DisplayName("不同类型和原因添加黑名单")
        void addToBlacklist_DifferentTypes(Integer type, String value, String reason) {
            when(blacklistMapper.insert(any(Blacklist.class))).thenReturn(1);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            assertDoesNotThrow(() -> blacklistService.addToBlacklist(type, value, reason));

            verify(blacklistMapper).insert(any(Blacklist.class));
        }
    }

    @Nested
    @DisplayName("移除黑名单测试")
    class RemoveBlacklistTests {

        @Test
        @DisplayName("移除手机号黑名单")
        void removeFromBlacklist_Phone_Success() {
            when(blacklistMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            blacklistService.removeFromBlacklist(1, "13800138000");

            verify(blacklistMapper).delete(any(LambdaQueryWrapper.class));
            verify(setOperations).remove("blacklist:phone", "13800138000");
        }

        @Test
        @DisplayName("移除身份证黑名单")
        void removeFromBlacklist_IdCard_Success() {
            when(blacklistMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            blacklistService.removeFromBlacklist(2, "110101199001011234");

            verify(blacklistMapper).delete(any(LambdaQueryWrapper.class));
            verify(setOperations).remove("blacklist:idcard", "110101199001011234");
        }

        @Test
        @DisplayName("移除不存在的黑名单记录")
        void removeFromBlacklist_NotExist_NoError() {
            when(blacklistMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            assertDoesNotThrow(() -> blacklistService.removeFromBlacklist(1, "99999999999"));

            verify(setOperations).remove("blacklist:phone", "99999999999");
        }
    }

    @Nested
    @DisplayName("获取黑名单列表测试")
    class GetBlacklistTests {

        @Test
        @DisplayName("获取所有黑名单")
        void getBlacklist_All_Success() {
            when(blacklistMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(phoneBlacklist, idCardBlacklist));

            List<Blacklist> result = blacklistService.getBlacklist(null);

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("只获取手机号黑名单")
        void getBlacklist_PhoneOnly_Success() {
            when(blacklistMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(phoneBlacklist));

            List<Blacklist> result = blacklistService.getBlacklist(1);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getType());
        }

        @Test
        @DisplayName("只获取身份证黑名单")
        void getBlacklist_IdCardOnly_Success() {
            when(blacklistMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(idCardBlacklist));

            List<Blacklist> result = blacklistService.getBlacklist(2);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(2, result.get(0).getType());
        }

        @Test
        @DisplayName("黑名单为空")
        void getBlacklist_Empty() {
            when(blacklistMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            List<Blacklist> result = blacklistService.getBlacklist(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("空手机号检测")
        void isPhoneBlacklisted_EmptyPhone() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember("blacklist:phone", "")).thenReturn(false);
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            boolean result = blacklistService.isPhoneBlacklisted("");

            assertFalse(result);
        }

        @Test
        @DisplayName("空身份证检测")
        void isIdCardBlacklisted_EmptyIdCard() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember("blacklist:idcard", "")).thenReturn(false);
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            boolean result = blacklistService.isIdCardBlacklisted("");

            assertFalse(result);
        }

        @Test
        @DisplayName("长原因文本添加黑名单")
        void addToBlacklist_LongReason() {
            String longReason = "a".repeat(500);
            when(blacklistMapper.insert(any(Blacklist.class))).thenReturn(1);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);

            assertDoesNotThrow(() -> blacklistService.addToBlacklist(1, "13800138000", longReason));
        }
    }
}
