package com.jbm.util.sensitive;

/**
 * 脱敏上下文：当前请求跳过脱敏（如查看本人完整信息）
 */
public final class SensitiveContext {

    private static final ThreadLocal<Boolean> SKIP_MASK = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private SensitiveContext() {
    }

    public static void skipMask() {
        SKIP_MASK.set(Boolean.TRUE);
    }

    public static void clear() {
        SKIP_MASK.remove();
    }

    public static boolean shouldSkipMask() {
        return Boolean.TRUE.equals(SKIP_MASK.get());
    }
}
