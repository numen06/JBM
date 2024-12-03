package test.cache;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.jbm.util.cache.CacheMonitor;
import com.jbm.util.cache.Caches;
import com.jbm.util.cache.CachesMonitor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.UUID;

@Slf4j
public class CacheMonitorTest {

    LoadingCache<String,String> cache =  Caches.getCachePool()
            .createLoadingCache(
                    (caffeine) -> caffeine.maximumSize(1000),
                    (key) -> UUID.randomUUID().toString());

    LoadingCache<String,String> cache2 =  Caches.getCachePool()
            .createLoadingCache(
                    (caffeine) -> caffeine.maximumSize(1000),
                    (key) -> IdUtil.fastSimpleUUID());

    @Test
    public void test() {
        CacheMonitor<String,String> cacheMonitor = new CacheMonitor<>(
                cache,()-> Lists.newArrayList("1", "2", "3"));
        cacheMonitor.start();
        int size = 3;
        for (int i = 0; i < size; i++) {
           cache.get(i+"");
        }
        while (true) {
            for (int i = 0; i < size; i++) {
                log.info("cache key:{},value:{}", i + "", cache.get(i + ""));
                ThreadUtil.sleep(1000);
            }
        }
//        ThreadUtil.waitForDie();
    }

    @Test
    public void testMore() {
        CachesMonitor<String> cachesMonitor = new CachesMonitor<>(
             ()-> Lists.newArrayList("1", "2", "3"));
        cachesMonitor.addCache(cache,(k)-> k);
        cachesMonitor.addCache(cache2,(k)-> k);
        cachesMonitor.start();
        int size = 3;
        for (int i = 0; i < size; i++) {
            cache.get(i+"");
            cache2.get(i+"");
        }
        while (true) {
            for (int i = 0; i < size; i++) {
                log.info("cache key:{},value:{}", i + "", cache.get(i + ""));
                log.info("cache2 key:{},value:{}", i + "", cache2.get(i + ""));
                ThreadUtil.sleep(1000);
            }
            log.info("--------------------------------");
        }
//        ThreadUtil.waitForDie();
    }
}
