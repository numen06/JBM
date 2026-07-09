package com.jbm.cluster.common.security.context;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 内部调用上下文，用于在内部服务链路上跳过用户权限注解校验。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InnerAuthContext {

    private static final ThreadLocal<Boolean> INNER_AUTH_VALID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SKIP_PERMISSION_CHECK = new ThreadLocal<>();

    public static void setValidated(boolean validated) {
        INNER_AUTH_VALID.set(validated);
    }

    public static boolean isValidated() {
        return Boolean.TRUE.equals(INNER_AUTH_VALID.get());
    }

    public static void setSkipPermissionCheck(boolean skip) {
        SKIP_PERMISSION_CHECK.set(skip);
    }

    public static boolean shouldSkipPermissionCheck() {
        return Boolean.TRUE.equals(SKIP_PERMISSION_CHECK.get());
    }

    public static void clear() {
        INNER_AUTH_VALID.remove();
        SKIP_PERMISSION_CHECK.remove();
    }
}
