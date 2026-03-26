package com.xindai.xindai.modules.agent.tools;

/**
 * ThreadLocal 上下文桥接器
 * LangChain4j Tool 是无状态的，通过 ThreadLocal 将当前用户身份传递给 Tool
 */
public final class ToolContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> ENTERPRISE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_PORTAL = new ThreadLocal<>();

    private ToolContext() {}

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static void setEnterpriseId(Long enterpriseId) {
        ENTERPRISE_ID.set(enterpriseId);
    }

    public static Long getUserId() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new IllegalStateException("ToolContext userId not set");
        }
        return id;
    }

    public static Long getEnterpriseId() {
        return ENTERPRISE_ID.get();
    }

    public static void setPortal(String portal) {
        CURRENT_PORTAL.set(portal);
    }

    public static String getPortal() {
        return CURRENT_PORTAL.get();
    }

    public static void clear() {
        USER_ID.remove();
        ENTERPRISE_ID.remove();
        CURRENT_PORTAL.remove();
    }
}
