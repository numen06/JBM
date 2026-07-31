package com.jbm.util.sensitive;

import java.util.EnumSet;

/**
 * 脱敏上下文：当前请求跳过全部或指定类型的脱敏
 */
public final class SensitiveContext {

    private static final ThreadLocal<Boolean> SKIP_MASK = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<EnumSet<SensitiveType>> SKIP_TYPES =
            ThreadLocal.withInitial(() -> EnumSet.noneOf(SensitiveType.class));

    private SensitiveContext() {
    }

    public static void skipMask() {
        SKIP_MASK.set(Boolean.TRUE);
    }

    public static void skipMask(SensitiveType type) {
        SKIP_TYPES.get().add(type);
    }

    public static void clear() {
        SKIP_MASK.remove();
        SKIP_TYPES.remove();
    }

    public static boolean shouldSkipMask() {
        return Boolean.TRUE.equals(SKIP_MASK.get());
    }

    public static boolean shouldSkipMask(SensitiveType type) {
        return shouldSkipMask() || SKIP_TYPES.get().contains(type);
    }
}
