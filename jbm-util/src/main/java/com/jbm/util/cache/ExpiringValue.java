package com.jbm.util.cache;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * A wrapper class for a value that expires after a certain time.
 *
 * @param <T>
 * @author wesley
 */
public class ExpiringValue<T> implements Supplier<T> {
    private final T value;
    private final Instant expiration;

    public ExpiringValue(T value, int ttlInSeconds) {
        this.value = value;
        this.expiration = Instant.now().plusSeconds(ttlInSeconds);
    }

    @Override
    public T get() {
        return value;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiration);
    }

    public static <T> ExpiringValue<T> of(T value) {
        return new ExpiringValue<>(value, 60);
    }

    public static <T> ExpiringValue<T> of(T value, int ttlInSeconds) {
        return new ExpiringValue<>(value, ttlInSeconds);
    }
}