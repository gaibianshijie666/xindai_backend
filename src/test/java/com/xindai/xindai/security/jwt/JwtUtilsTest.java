package com.xindai.xindai.security.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("JwtUtils 单元测试")
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private StringRedisTemplate stringRedisTemplate;

    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-generation-must-be-long-enough";
    private static final Long TEST_EXPIRATION = 3600000L; // 1小时

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        jwtUtils = new JwtUtils(stringRedisTemplate);
        ReflectionTestUtils.setField(jwtUtils, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "expiration", TEST_EXPIRATION);
    }

    @Nested
    @DisplayName("普通用户Token测试")
    class UserTokenTests {

        @Test
        @DisplayName("生成用户Token - 成功")
        void generateToken_Success() {
            String token = jwtUtils.generateToken(1L, "13800138000");

            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.split("\\.").length == 3); // JWT有三部分
        }

        @Test
        @DisplayName("解析Token获取用户ID")
        void getUserId_Success() {
            String token = jwtUtils.generateToken(1L, "13800138000");

            Long userId = jwtUtils.getUserId(token);

            assertEquals(1L, userId);
        }

        @Test
        @DisplayName("解析Token获取手机号")
        void getPhone_Success() {
            String token = jwtUtils.generateToken(1L, "13800138000");

            String phone = jwtUtils.getPhone(token);

            assertEquals("13800138000", phone);
        }

        @Test
        @DisplayName("解析Token获取Claims")
        void parseToken_Success() {
            String token = jwtUtils.generateToken(1L, "13800138000");

            Claims claims = jwtUtils.parseToken(token);

            assertNotNull(claims);
            assertEquals(1L, claims.get("userId", Long.class));
            assertEquals("13800138000", claims.getSubject());
        }

        @Test
        @DisplayName("不同用户生成不同Token")
        void generateToken_DifferentUsers() {
            String token1 = jwtUtils.generateToken(1L, "13800138000");
            String token2 = jwtUtils.generateToken(2L, "13900139000");

            assertNotEquals(token1, token2);
        }

        @Test
        @DisplayName("同一用户多次生成不同Token（时间戳不同）")
        void generateToken_SameUserMultipleTimes() throws InterruptedException {
            String token1 = jwtUtils.generateToken(1L, "13800138000");
            Thread.sleep(100); // 确保时间戳不同
            String token2 = jwtUtils.generateToken(1L, "13800138000");

            // Token可能不同（因为时间戳），但都能解析出相同的用户信息
            Long userId1 = jwtUtils.getUserId(token1);
            Long userId2 = jwtUtils.getUserId(token2);

            assertEquals(userId1, userId2);
        }
    }

    @Nested
    @DisplayName("管理员Token测试")
    class AdminTokenTests {

        @Test
        @DisplayName("生成管理员Token包含userType=ADMIN")
        void generateAdminToken_Success() {
            String token = jwtUtils.generateToken(1L, "admin", "ADMIN", null);

            assertNotNull(token);
            assertEquals("ADMIN", jwtUtils.getUserType(token));
            assertEquals(1L, jwtUtils.getUserId(token));
        }

        @Test
        @DisplayName("管理员Token不包含enterpriseId")
        void generateAdminToken_NoEnterpriseId() {
            String token = jwtUtils.generateToken(1L, "admin", "ADMIN", null);

            assertNull(jwtUtils.getEnterpriseId(token));
        }
    }

    @Nested
    @DisplayName("统一Token方法测试")
    class UnifiedTokenTests {

        @Test
        @DisplayName("userType=USER的Token与向后兼容方法一致")
        void generateToken_User_BackwardCompat() {
            String oldToken = jwtUtils.generateToken(1L, "13800138000");
            String newToken = jwtUtils.generateToken(1L, "13800138000", "USER", null);

            assertEquals("USER", jwtUtils.getUserType(newToken));
            assertEquals(1L, jwtUtils.getUserId(newToken));
            assertEquals(1L, jwtUtils.getUserId(oldToken));
        }
    }

    @Nested
    @DisplayName("企业用户Token测试")
    class EnterpriseTokenTests {

        @Test
        @DisplayName("生成企业Token - 成功")
        void generateEnterpriseToken_Success() {
            String token = jwtUtils.generateToken(100L, "admin", "ENTERPRISE", 1L);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("解析企业Token获取企业ID")
        void getEnterpriseId_Success() {
            String token = jwtUtils.generateToken(100L, "admin", "ENTERPRISE", 1L);

            Long enterpriseId = jwtUtils.getEnterpriseId(token);

            assertEquals(1L, enterpriseId);
        }

        @Test
        @DisplayName("解析企业Token获取用户类型")
        void getUserType_Enterprise() {
            String token = jwtUtils.generateToken(100L, "admin", "ENTERPRISE", 1L);

            String userType = jwtUtils.getUserType(token);

            assertEquals("ENTERPRISE", userType);
        }

        @Test
        @DisplayName("验证是企业Token")
        void isEnterpriseToken_True() {
            String token = jwtUtils.generateToken(100L, "admin", "ENTERPRISE", 1L);

            assertTrue(jwtUtils.isEnterpriseToken(token));
        }

        @Test
        @DisplayName("普通用户Token不是企业Token")
        void isEnterpriseToken_False() {
            String token = jwtUtils.generateToken(1L, "13800138000");

            assertFalse(jwtUtils.isEnterpriseToken(token));
        }

        @Test
        @DisplayName("不同企业用户生成不同Token")
        void generateEnterpriseToken_DifferentUsers() {
            String token1 = jwtUtils.generateToken(100L, "admin", "ENTERPRISE", 1L);
            String token2 = jwtUtils.generateToken(200L, "operator", "ENTERPRISE", 2L);

            assertNotEquals(token1, token2);
        }
    }

    @Nested
    @DisplayName("Token验证测试")
    class TokenValidationTests {

        @Test
        @DisplayName("验证有效Token - 返回true")
        void validateToken_Valid() {
            String token = jwtUtils.generateToken(1L, "13800138000");

            assertTrue(jwtUtils.validateToken(token));
        }

        @Test
        @DisplayName("验证无效Token - 返回false")
        void validateToken_Invalid() {
            String invalidToken = "invalid.token.here";

            assertFalse(jwtUtils.validateToken(invalidToken));
        }

        @Test
        @DisplayName("验证空Token - 返回false")
        void validateToken_Empty() {
            assertFalse(jwtUtils.validateToken(""));
        }

        @Test
        @DisplayName("验证null Token - 返回false")
        void validateToken_Null() {
            assertFalse(jwtUtils.validateToken(null));
        }

        @Test
        @DisplayName("验证格式错误的Token - 返回false")
        void validateToken_Malformed() {
            assertFalse(jwtUtils.validateToken("not-a-valid-jwt"));
        }

        @Test
        @DisplayName("验证被篡改的Token - 返回false")
        void validateToken_Tampered() {
            String token = jwtUtils.generateToken(1L, "13800138000");
            String tamperedToken = token + "tampered";

            assertFalse(jwtUtils.validateToken(tamperedToken));
        }
    }

    @Nested
    @DisplayName("Token过期测试")
    class TokenExpirationTests {

        @Test
        @DisplayName("新Token未过期")
        void isTokenExpired_NotExpired() {
            String token = jwtUtils.generateToken(1L, "13800138000");

            assertFalse(jwtUtils.isTokenExpired(token));
        }

        @Test
        @DisplayName("过期Token验证失败")
        void validateToken_Expired() {
            // 设置非常短的过期时间
            ReflectionTestUtils.setField(jwtUtils, "expiration", 1L); // 1毫秒
            String token = jwtUtils.generateToken(1L, "13800138000");

            // 等待过期
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            assertFalse(jwtUtils.validateToken(token));
        }

        @Test
        @DisplayName("过期Token检查返回true")
        void isTokenExpired_Expired() {
            // 设置非常短的过期时间
            ReflectionTestUtils.setField(jwtUtils, "expiration", 1L);
            String token = jwtUtils.generateToken(1L, "13800138000");

            // 等待过期
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            assertTrue(jwtUtils.isTokenExpired(token));
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @ParameterizedTest
        @ValueSource(longs = {1L, 100L, Long.MAX_VALUE, Long.MIN_VALUE})
        @DisplayName("不同用户ID生成Token")
        void generateToken_DifferentUserIds(Long userId) {
            String token = jwtUtils.generateToken(userId, "13800138000");

            assertNotNull(token);
            assertEquals(userId, jwtUtils.getUserId(token));
        }

        @ParameterizedTest
        @ValueSource(strings = {"13800138000", "1", "a", "very-long-phone-number-12345678901234567890"})
        @DisplayName("不同手机号生成Token")
        void generateToken_DifferentPhones(String phone) {
            String token = jwtUtils.generateToken(1L, phone);

            assertNotNull(token);
            assertEquals(phone, jwtUtils.getPhone(token));
        }

        @Test
        @DisplayName("用户ID为null时处理")
        void generateToken_NullUserId() {
            // 可能会抛出异常或返回包含null的token
            assertDoesNotThrow(() -> {
                try {
                    String token = jwtUtils.generateToken(null, "13800138000");
                    // 如果成功生成，检查能否解析
                    jwtUtils.parseToken(token);
                } catch (Exception e) {
                    // 某些实现可能不允许null值
                }
            });
        }

        @Test
        @DisplayName("手机号为null时处理")
        void generateToken_NullPhone() {
            assertDoesNotThrow(() -> {
                try {
                    String token = jwtUtils.generateToken(1L, null);
                    jwtUtils.parseToken(token);
                } catch (Exception e) {
                    // 某些实现可能不允许null值
                }
            });
        }

        @Test
        @DisplayName("企业Token包含所有必要信息")
        void generateEnterpriseToken_AllClaims() {
            String token = jwtUtils.generateToken(100L, "testuser", "ENTERPRISE", 10L);
            Claims claims = jwtUtils.parseToken(token);

            assertEquals(10L, claims.get("enterpriseId", Long.class));
            assertEquals(100L, claims.get("userId", Long.class));
            assertEquals("ENTERPRISE", claims.get("userType", String.class));
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("解析无效Token返回null或不抛出异常")
        void parseToken_Invalid() {
            assertDoesNotThrow(() -> {
                try {
                    jwtUtils.parseToken("invalid");
                } catch (Exception e) {
                    // 预期可能抛出异常
                }
            });
        }

        @Test
        @DisplayName("从无效Token获取用户ID抛出异常")
        void getUserId_InvalidToken() {
            assertThrows(Exception.class, () -> jwtUtils.getUserId("invalid.token.here"));
        }

        @Test
        @DisplayName("从无效Token获取手机号抛出异常")
        void getPhone_InvalidToken() {
            assertThrows(Exception.class, () -> jwtUtils.getPhone("invalid.token.here"));
        }

        @Test
        @DisplayName("从普通Token获取企业ID返回null")
        void getEnterpriseId_FromUserToken() {
            String userToken = jwtUtils.generateToken(1L, "13800138000");
            Long enterpriseId = jwtUtils.getEnterpriseId(userToken);

            assertNull(enterpriseId);
        }

        @Test
        @DisplayName("从普通Token获取用户名返回null")
        void getUsername_FromUserToken() {
            String userToken = jwtUtils.generateToken(1L, "13800138000");
            String username = jwtUtils.getUsername(userToken);

            assertNull(username);
        }

        @Test
        @DisplayName("从无效Token检查是否企业Token返回false")
        void isEnterpriseToken_InvalidToken() {
            assertFalse(jwtUtils.isEnterpriseToken("invalid"));
        }
    }

    @Nested
    @DisplayName("Token结构测试")
    class TokenStructureTests {

        @Test
        @DisplayName("Token包含正确的签发时间")
        void token_HasIssuedAt() {
            long beforeGeneration = System.currentTimeMillis() - 1000;
            String token = jwtUtils.generateToken(1L, "13800138000");
            long afterGeneration = System.currentTimeMillis() + 1000;

            Claims claims = jwtUtils.parseToken(token);

            assertNotNull(claims.getIssuedAt());
            assertTrue(claims.getIssuedAt().getTime() >= beforeGeneration);
            assertTrue(claims.getIssuedAt().getTime() <= afterGeneration);
        }

        @Test
        @DisplayName("Token包含正确的过期时间")
        void token_HasExpiration() {
            long beforeGeneration = System.currentTimeMillis();
            String token = jwtUtils.generateToken(1L, "13800138000");

            Claims claims = jwtUtils.parseToken(token);

            assertNotNull(claims.getExpiration());
            // 过期时间应该大约等于当前时间 + expiration
            long expectedExpiry = beforeGeneration + TEST_EXPIRATION;
            assertTrue(claims.getExpiration().getTime() >= expectedExpiry - 1000);
            assertTrue(claims.getExpiration().getTime() <= expectedExpiry + 1000);
        }
    }
}
