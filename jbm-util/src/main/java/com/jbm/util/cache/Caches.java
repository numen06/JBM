package com.jbm.util.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wesley
 */
@Slf4j
public class Caches {

    private final static Cache<String, CachePool> CACHE_POOLS = Caffeine.newBuilder().build();

    private final static String DEFAULT_GROUP = "default";

    public static CachePool getCachePool() {
        return CACHE_POOLS.get(DEFAULT_GROUP, k -> new CachePool());
    }

    public static CachePool getCachePool(String group) {
        return CACHE_POOLS.get(group, k -> new CachePool());
    }

    public static void clear(String group) {
        CACHE_POOLS.invalidate(group);
    }


    public static <K, T> void putExpiredValue(Cache<K, ExpiringValue<T>> cache, K key, T value, int expireTime) {
        putExpiredValue(cache, key, new ExpiringValue<>(value, expireTime));
    }

    public static <K, T> void putExpiredValue(Cache<K, ExpiringValue<T>> cache, K key, ExpiringValue<T> expiringValue) {
        cache.put(key, expiringValue);
    }

    /**
     * 获取值并检查是否过期
     *
     * @param cache
     * @param key
     * @param <T>
     * @return
     */
    public static <K, T> T getIfNotExpired(Cache<K, ExpiringValue<T>> cache, String key) {
        return getIfNotExpired(cache, key, null);
    }

    public static <K, T> T getIfNotExpired(Cache<K, ExpiringValue<T>> cache, String key, T defaultValue) {
        ExpiringValue<T> expiringValue = cache.getIfPresent(key);
        if (expiringValue != null && !expiringValue.isExpired()) {
            return expiringValue.get();
        }
        // 如果过期，删除缓存项
        if (expiringValue != null && expiringValue.isExpired()) {
            cache.invalidate(key);
        }
        return defaultValue;
    }

}
