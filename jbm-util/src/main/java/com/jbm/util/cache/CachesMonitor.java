package com.jbm.util.cache;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * 缓存监控器
 * @author wesley
 */
@Slf4j
public class CachesMonitor<T> {

    private final List<CacheMonitorBean<?,?>> caches = new ArrayList<>();
    private final Callable<List<T>> ckeckKeysCallable;
    // 定时任务表达式
    private final String cron;

    public CachesMonitor( Callable<List<T>> ckeckKeysCallable) {
        this( ckeckKeysCallable, null);
    }

    public CachesMonitor( Callable<List<T>> ckeckKeysCallable, String cron) {
        this.ckeckKeysCallable = ckeckKeysCallable;
        // 默认每5秒执行一次
        this.cron = StrUtil.isBlank(cron) ? "0/5 * * * * ?" : cron;
    }

    public  class CacheMonitorBean<K,V>   {
        private final Cache<K, V> cache;
        private final Function<T,  K> function;

        public CacheMonitorBean(Cache<K, V> cache, Function<T, K> function) {
            this.cache = cache;
            this.function = function;
        }

        public void reload(T bean) {
            this.reloadByKey(function.apply(bean));
        }

        /**
         * 丢失缓存重新获取
         * @param key
         */
        public void reloadByKey(K key) {
            if (ObjectUtil.isNull(key) ) {
                return;
            }
            // 移除缓存
            cache.invalidate(key);
            // 重新获取缓存
            cache.getIfPresent(key);
        }

        /**
         * 更新缓存
         * @param key
         * @param value
         */
        public void update(K key, V value) {
            cache.put(key, value);
        }
    }

    public <K,V> void addCache(Cache<K,V> cache,Function<T,K> function) {
        caches.add(new CacheMonitorBean<>(cache, function));
    }


    /**
     * 启动
     */
    public void start() {
        CronUtil.schedule(this.cron, (Task) this::checkTask);
        // 支持秒级别定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

    /**
     * 检查任务
     */
    protected void checkTask() {
        try {
            List<T> lostKeys = ckeckKeysCallable.call();
            if (CollUtil.isEmpty(lostKeys)) {
                return;
            }
            lostKeys.forEach(bean -> caches.forEach(cache -> cache.reload(bean)));
            log.info("check cache success, lostKeys:{}", lostKeys);
        } catch (Exception e) {
            log.error("check cache error", e);
        }
    }

}
