package com.xindai.xindai.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private final StringRedisTemplate stringRedisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成Token（完整版，支持所有用户类型）
     *
     * @param userId      用户ID
     * @param identifier  标识符（用户手机号/管理员用户名/企业用户名）
     * @param userType    用户类型：USER, ADMIN, ENTERPRISE
     * @param enterpriseId 企业ID（仅企业用户需要，其他传null）
     */
    public String generateToken(Long userId, String identifier, String userType, Long enterpriseId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userType", userType);
        if (enterpriseId != null) {
            claims.put("enterpriseId", enterpriseId);
        }
        return createToken(claims, identifier);
    }

    /**
     * 向后兼容：普通用户Token生成
     */
    public String generateToken(Long userId, String phone) {
        return generateToken(userId, phone, "USER", null);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    public String getPhone(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            if (isTokenExpired(token)) {
                return false;
            }
            Long userId = getUserId(token);
            if (isTokenInvalidated(userId)) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void invalidateToken(Long userId) {
        String key = TOKEN_BLACKLIST_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(key, "1", expiration, TimeUnit.MILLISECONDS);
    }

    public boolean isTokenInvalidated(Long userId) {
        String key = TOKEN_BLACKLIST_PREFIX + userId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    public String getUserType(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("userType", String.class) : null;
    }

    public Long getEnterpriseId(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("enterpriseId", Long.class) : null;
    }

    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("username", String.class) : null;
    }

    public boolean isEnterpriseToken(String token) {
        try {
            String userType = getUserType(token);
            return "ENTERPRISE".equals(userType) || "enterprise".equals(userType);
        } catch (Exception e) {
            return false;
        }
    }
}
