package com.jbm.cluster.center.integration.support;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * H2 集成测试：提供 Redis 相关 Bean 占位，避免连接真实 Redis。
 */
@TestConfiguration
public class CenterH2TestConfiguration {

    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        return Mockito.mock(StringRedisTemplate.class);
    }
}
