package com.jbm.util.cache;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Callable;


/**
 * 缓存监控器
 *
 * @author wesley
 */
@Slf4j
public class CacheMonitor<K, V> {

    private final Cache<K, V> cache;
    private final Callable<List<K>> ckeckKeysCallable;
    // 定时任务表达式
    private final String cron;

    public CacheMonitor(Cache<K, V> cache, Callable<List<K>> ckeckKeysCallable) {
        this(cache, ckeckKeysCallable, null);
    }

    public CacheMonitor(Cache<K, V> cache, Callable<List<K>> ckeckKeysCallable, String cron) {
        this.cache = cache;
        this.ckeckKeysCallable = ckeckKeysCallable;
        // 默认每5秒执行一次
        this.cron = StrUtil.isBlank(cron) ? "0/5 * * * * ?" : cron;
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
     * 丢失缓存重新获取
     *
     * @param key
     */
    public void reload(K key) {
        if (ObjectUtil.isNull(key)) {
            return;
        }
        // 移除缓存
        cache.invalidate(key);
        // 重新获取缓存
        cache.getIfPresent(key);
    }

    /**
     * 更新缓存
     *
     * @param key
     * @param value
     */
    public void update(K key, V value) {
        cache.put(key, value);
    }

    /**
     * 检查任务
     */
    protected void checkTask() {
        try {
            List<K> lostKeys = ckeckKeysCallable.call();
            if (CollUtil.isEmpty(lostKeys)) {
                return;
            }
            for (K key : lostKeys) {
                reload(key);
            }
            log.info("cache size:{},lost size:{}", cache.estimatedSize(), lostKeys.size());
        } catch (Exception e) {
            log.error("check cache error", e);
        }
    }

}
