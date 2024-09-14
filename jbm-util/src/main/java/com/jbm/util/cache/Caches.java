package com.jbm.util.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

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


}
