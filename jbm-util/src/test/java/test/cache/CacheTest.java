package test.cache;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.IdUtil;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.jbm.util.cache.Caches;
import com.jbm.util.key.KeyBean;
import com.jbm.util.key.KeyObject;
import com.jbm.util.key.Keys;
import org.junit.Test;

import java.util.UUID;

public class CacheTest  {


    @Test
    public void test() {
       LoadingCache<String,String> cache =  Caches.getCachePool()
               .createLoadingCache(
                       (caffeine) -> caffeine.maximumSize(1000),
               (key) -> UUID.randomUUID().toString());
        cache.put("key", "value");
        System.out.println(cache.get("key"));
    }


    @Test
    public void testKey() {
        LoadingCache<KeyObject,String> cache =  Caches.getCachePool()
                .createLoadingCache(
                        (caffeine) -> caffeine.maximumSize(1000),
                        (key) -> UUID.randomUUID().toString());
        String val = IdUtil.fastSimpleUUID();
        cache.put(Keys.of("test"),val );
        Console.log("val:{}",val);
        Console.log("cache:{}",cache.get(Keys.of("test")));
    }

    @Test
    public void testKey2() {
        LoadingCache<KeyBean<String>,String> cache =  Caches.getCachePool()
                .createLoadingCache(
                        (caffeine) -> caffeine.maximumSize(1000),
                        (key) -> UUID.randomUUID().toString());
        String val = IdUtil.fastSimpleUUID();
        cache.put(Keys.ofBean("test"),val );
        Console.log("val:{}",val);
        Console.log("cache:{}",cache.get(Keys.ofBean("test")));
    }
}
