package com.xindai.xindai.modules.agent.interceptor;

import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.agent.annotation.ToolAllowed;
import com.xindai.xindai.modules.agent.tools.ToolContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class ToolPermissionInterceptor {

    @Around("@annotation(toolAllowed)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, ToolAllowed toolAllowed) throws Throwable {
        String currentPortal = ToolContext.getPortal();
        if (currentPortal == null || currentPortal.isBlank()) {
            log.warn("Tool call without portal context, denying access");
            throw new BusinessException(ErrorCode.AGENT_CONTEXT_ERROR);
        }

        boolean allowed = Arrays.asList(toolAllowed.portals()).contains(currentPortal);
        if (!allowed) {
            log.warn("Tool access denied: portal={}, method={}", currentPortal, joinPoint.getSignature().getName());
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return joinPoint.proceed();
    }
}
