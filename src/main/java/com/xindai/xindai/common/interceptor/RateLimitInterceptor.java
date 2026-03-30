package com.xindai.xindai.common.interceptor;

import com.xindai.xindai.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.Collections;
import java.util.List;

/**
 * 基于 Redis 的请求限流拦截器
 * 使用滑动窗口算法实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${rate-limit.default-limit:100}")
    private int defaultLimit;

    @Value("${rate-limit.default-period:60}")
    private int defaultPeriod;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * Lua脚本：原子性地执行increment和expire操作
     * 返回值：[当前计数, 剩余TTL]
     */
    private static final String RATE_LIMIT_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            local ttl = redis.call('TTL', KEYS[1])
            return {current, ttl}
            """;

    /**
     * 特定路径的限流配置
     */
    private static final Map<String, RateLimitConfig> PATH_CONFIGS = new HashMap<>();

    static {
        PATH_CONFIGS.put("/api/v1/user/login", new RateLimitConfig(10, 60));
        PATH_CONFIGS.put("/api/v1/user/register", new RateLimitConfig(5, 60));
        PATH_CONFIGS.put("/api/v1/loan/apply", new RateLimitConfig(10, 60));
        PATH_CONFIGS.put("/api/v1/enterprise/auth/login", new RateLimitConfig(10, 60));
        PATH_CONFIGS.put("/api/v1/enterprise/loans/apply", new RateLimitConfig(10, 60));
        PATH_CONFIGS.put("/api/v1/enterprise/loans/batch-apply", new RateLimitConfig(5, 60));
        PATH_CONFIGS.put("/api/v1/enterprise/credit/apply", new RateLimitConfig(3, 60));
        PATH_CONFIGS.put("/api/v1/agent/user/chat/stream", new RateLimitConfig(20, 60));
        PATH_CONFIGS.put("/api/v1/agent/enterprise/chat/stream", new RateLimitConfig(20, 60));
        PATH_CONFIGS.put("/api/v1/agent/admin/chat/stream", new RateLimitConfig(20, 60));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled) {
            return true;
        }

        String path = request.getRequestURI();
        String clientId = getClientIdentifier(request);

        // 获取该路径的限流配置
        RateLimitConfig config = PATH_CONFIGS.getOrDefault(path, new RateLimitConfig(defaultLimit, defaultPeriod));

        String key = RATE_LIMIT_PREFIX + path + ":" + clientId;

        // 使用Lua脚本原子性地执行increment和expire操作
        long currentCount = 0L;
        long ttl = config.period;

        try {
            DefaultRedisScript<List> script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, List.class);
            List<?> rawResult = redisTemplate.execute(script, Collections.singletonList(key), String.valueOf(config.period));

            // 解析结果
            if (rawResult != null && !rawResult.isEmpty()) {
                Object first = rawResult.get(0);
                currentCount = first instanceof Number ? ((Number) first).longValue() : 0L;
                if (rawResult.size() > 1) {
                    Object second = rawResult.get(1);
                    ttl = second instanceof Number ? ((Number) second).longValue() : config.period;
                }
            }
        } catch (Exception e) {
            // Redis 错误不应阻止正常请求，记录日志后放行
            log.error("Rate limit check failed for path {}: {}", path, e.getMessage());
            return true;
        }

        if (currentCount > config.limit) {
            log.warn("Rate limit exceeded for client {} on path {}: {}/{} requests",
                    clientId, path, currentCount, config.limit);

            // 设置响应头
            response.setHeader("X-RateLimit-Limit", String.valueOf(config.limit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(ttl));
            response.setHeader("Retry-After", String.valueOf(ttl));

            // 返回 429 错误
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");

            Result<Void> errorResult = Result.error(429, "请求过于频繁，请稍后再试");
            response.getWriter().write(objectMapper.writeValueAsString(errorResult));

            return false;
        }

        // 设置响应头
        response.setHeader("X-RateLimit-Limit", String.valueOf(config.limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, config.limit - currentCount)));

        return true;
    }

    /**
     * 获取客户端标识符
     * 优先使用用户ID，其次使用IP地址
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // 尝试从请求属性获取用户ID（由JWT过滤器设置）
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            return "user:" + userId;
        }

        // 使用IP地址
        String ip = getClientIp(request);
        return "ip:" + ip;
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // 只有来自可信代理时才信任X-Forwarded-For
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && isTrustedProxy(remoteAddr)) {
            // 取第一个非可信IP
            String[] ips = forwardedFor.split(",");
            for (String ip : ips) {
                ip = ip.trim();
                if (!isTrustedProxy(ip)) {
                    return ip;
                }
            }
            return ips[0].trim();
        }

        // 其他情况使用RemoteAddr
        return remoteAddr;
    }

    /**
     * 判断IP是否为可信代理
     * 内网IP视为可信代理
     */
    private boolean isTrustedProxy(String ip) {
        // 内网IP视为可信代理
        return ip.startsWith("10.") ||
               ip.startsWith("192.168.") ||
               ip.startsWith("172.16.") ||
               ip.startsWith("172.17.") ||
               ip.startsWith("172.18.") ||
               ip.startsWith("172.19.") ||
               ip.startsWith("172.20.") ||
               ip.startsWith("172.21.") ||
               ip.startsWith("172.22.") ||
               ip.startsWith("172.23.") ||
               ip.startsWith("172.24.") ||
               ip.startsWith("172.25.") ||
               ip.startsWith("172.26.") ||
               ip.startsWith("172.27.") ||
               ip.startsWith("172.28.") ||
               ip.startsWith("172.29.") ||
               ip.startsWith("172.30.") ||
               ip.startsWith("172.31.") ||
               ip.equals("127.0.0.1") ||
               ip.equals("0:0:0:0:0:0:0:1");
    }

    /**
     * 限流配置
     */
    private record RateLimitConfig(int limit, int period) {}
}
