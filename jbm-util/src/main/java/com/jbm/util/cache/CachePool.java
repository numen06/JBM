package com.jbm.util.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 缓存池
 *
 * @author wesley
 */
public class CachePool {


    private String name;

    private final List<Cache<?, ?>> cacheList = new ArrayList<>();

    public CachePool() {
    }

    public CachePool(String name) {
        this.name = name;
    }

    public <K, V> Cache<K, V> create(Function<Caffeine<Object, Object>, Caffeine<K, V>> function) {
        Cache<K, V> cache = function.apply(Caffeine.newBuilder()).build();
        this.cacheList.add(cache);
        return cache;
    }

    public <K, V> LoadingCache<K, V> createLoadingCache(Function<Caffeine<K, V>,  Caffeine<K, V>> function, Function<K, V> loader) {
        LoadingCache<K, V> cache = Caffeine.newBuilder().build(loader::apply);
        this.cacheList.add(cache);
        return cache;
    }

    public void clear() {
        cacheList.clear();
    }
}
