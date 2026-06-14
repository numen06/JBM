package com.jbm.cluster.job.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ShedLock：多实例下通过 Redis 保证同一任务同一时间仅一处执行。
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockProvider(redisConnectionFactory, "jbm-job");
    }
}
