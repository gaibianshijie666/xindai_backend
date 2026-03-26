package com.xindai.xindai.common.aspect;

import com.xindai.xindai.common.annotation.OperateLog;
import com.xindai.xindai.common.entity.OperationLog;
import com.xindai.xindai.common.service.AsyncOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperateLogAspect {

    private final AsyncOperationLogService asyncOperationLogService;

    @AfterReturning("@annotation(com.xindai.xindai.common.annotation.OperateLog)")
    public void recordLog(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            OperateLog operateLog = method.getAnnotation(OperateLog.class);

            OperationLog logEntity = new OperationLog();
            logEntity.setModule(operateLog.module());
            logEntity.setOperation(operateLog.operation());

            // Get user info from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String userType = "USER";
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String role = authority.getAuthority();
                    if (role.contains("ADMIN") && !role.contains("ENTERPRISE")) {
                        userType = "ADMIN";
                        break;
                    } else if (role.contains("ENTERPRISE")) {
                        userType = "ENTERPRISE";
                        break;
                    }
                }
                logEntity.setUserType(userType);
                Object principal = authentication.getPrincipal();
                if (principal instanceof Long) {
                    logEntity.setUserId((Long) principal);
                } else if (principal != null) {
                    try {
                        logEntity.setUserId(Long.valueOf(principal.toString()));
                    } catch (NumberFormatException ignored) {
                    }
                }
                logEntity.setUsername(authentication.getName());
            }

            // Get IP address (with trusted proxy check to prevent header spoofing)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = getClientIp(request);
                logEntity.setIpAddress(ip);
            }

            // Get traceId
            logEntity.setTraceId(MDC.get("traceId"));

            // Build detail from method info
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = method.getName();
            logEntity.setDetail(className + "." + methodName);

            asyncOperationLogService.save(logEntity);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    /**
     * 获取客户端真实IP（仅信任来自内网代理的X-Forwarded-For头）
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
}
