package jbm.framework.boot.autoconfigure.redis.cache;

import cn.hutool.core.util.StrUtil;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.lang.Nullable;

import java.time.Duration;

/**
 * @author wesley
 */
public class CustomizedRedisCacheManager extends RedisCacheManager {

    public CustomizedRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
    }

    @Override
    protected RedisCache createRedisCache(@Nullable String name, @Nullable RedisCacheConfiguration cacheConfig) {
        String[] array = StrUtil.splitToArray(name, "#");
        name = array[0];
        if (array.length > 1) {
            long ttl = Long.parseLong(array[1]);
            if (cacheConfig != null) {
                cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(ttl));
            }
        }
        return super.createRedisCache(name, cacheConfig);
    }
}