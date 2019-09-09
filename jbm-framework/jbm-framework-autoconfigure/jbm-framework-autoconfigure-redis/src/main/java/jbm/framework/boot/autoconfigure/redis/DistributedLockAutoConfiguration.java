package jbm.framework.boot.autoconfigure.redis;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-09-09 19:19
 **/
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class DistributedLockAutoConfiguration {

    @Bean
    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockRegistry(redisConnectionFactory, "JBM");
    }

}