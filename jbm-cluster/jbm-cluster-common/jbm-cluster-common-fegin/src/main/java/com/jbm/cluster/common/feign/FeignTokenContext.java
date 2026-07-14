package com.jbm.cluster.common.feign;

import java.util.function.Supplier;

/**
 * Feign token mode context. Default calls use a service ClientToken; relay mode copies the current inbound token.
 */
public final class FeignTokenContext {

    private static final String LEGACY_ACCESS_MODE_USER = "user";

    public static final String ACCESS_MODE_HEADER = "x-jbm-feign-access-mode";
    public static final String ACCESS_MODE_RELAY = "relay";
    /**
     * @deprecated Use {@link #ACCESS_MODE_RELAY}. Kept for callers compiled against the legacy marker value.
     */
    @Deprecated
    public static final String ACCESS_MODE_USER = LEGACY_ACCESS_MODE_USER;
    public static final String ACCESS_MODE_INTERNAL = "internal";

    private static final ThreadLocal<String> ACCESS_MODE = new ThreadLocal<>();

    private FeignTokenContext() {
    }

    public static boolean isTokenRelay() {
        return isTokenRelayMode(ACCESS_MODE.get());
    }

    public static boolean isTokenRelayMode(String accessMode) {
        return ACCESS_MODE_RELAY.equalsIgnoreCase(accessMode) || LEGACY_ACCESS_MODE_USER.equalsIgnoreCase(accessMode);
    }

    public static void useTokenRelay() {
        ACCESS_MODE.set(ACCESS_MODE_RELAY);
    }

    public static void useInternalToken() {
        ACCESS_MODE.set(ACCESS_MODE_INTERNAL);
    }

    public static void clear() {
        ACCESS_MODE.remove();
    }

    public static void withTokenRelay(Runnable runnable) {
        withAccessMode(ACCESS_MODE_RELAY, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T withTokenRelay(Supplier<T> supplier) {
        return withAccessMode(ACCESS_MODE_RELAY, supplier);
    }

    public static <T> T withInternalToken(Supplier<T> supplier) {
        return withAccessMode(ACCESS_MODE_INTERNAL, supplier);
    }

    /**
     * @deprecated Use {@link #isTokenRelay()}.
     */
    @Deprecated
    public static boolean isUserTokenRelay() {
        return isTokenRelay();
    }

    /**
     * @deprecated Use {@link #useTokenRelay()}.
     */
    @Deprecated
    public static void useUserToken() {
        useTokenRelay();
    }

    /**
     * @deprecated Use {@link #withTokenRelay(Runnable)}.
     */
    @Deprecated
    public static void withUserToken(Runnable runnable) {
        withTokenRelay(runnable);
    }

    /**
     * @deprecated Use {@link #withTokenRelay(Supplier)}.
     */
    @Deprecated
    public static <T> T withUserToken(Supplier<T> supplier) {
        return withTokenRelay(supplier);
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
