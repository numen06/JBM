package com.jbm.cluster.common.feign;

import java.util.function.Supplier;

/**
 * Feign token mode context. Default calls use service ClientToken; wrap calls here to relay the current user token.
 */
public final class FeignTokenContext {

    public static final String ACCESS_MODE_HEADER = "x-jbm-feign-access-mode";
    public static final String ACCESS_MODE_USER = "user";
    public static final String ACCESS_MODE_INTERNAL = "internal";

    private static final ThreadLocal<String> ACCESS_MODE = new ThreadLocal<>();

    private FeignTokenContext() {
    }

    public static boolean isUserTokenRelay() {
        return ACCESS_MODE_USER.equals(ACCESS_MODE.get());
    }

    public static void useUserToken() {
        ACCESS_MODE.set(ACCESS_MODE_USER);
    }

    public static void useInternalToken() {
        ACCESS_MODE.set(ACCESS_MODE_INTERNAL);
    }

    public static void clear() {
        ACCESS_MODE.remove();
    }

    public static void withUserToken(Runnable runnable) {
        withAccessMode(ACCESS_MODE_USER, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T withUserToken(Supplier<T> supplier) {
        return withAccessMode(ACCESS_MODE_USER, supplier);
    }

    public static <T> T withInternalToken(Supplier<T> supplier) {
        return withAccessMode(ACCESS_MODE_INTERNAL, supplier);
    }

    private static <T> T withAccessMode(String accessMode, Supplier<T> supplier) {
        String previous = ACCESS_MODE.get();
        ACCESS_MODE.set(accessMode);
        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                ACCESS_MODE.remove();
            } else {
                ACCESS_MODE.set(previous);
            }
        }
    }
}
