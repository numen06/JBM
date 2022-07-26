package com.jbm.cluster.common.idempotent.config;

import com.jbm.cluster.common.idempotent.aspectj.RepeatSubmitAspect;
import jbm.framework.boot.autoconfigure.redis.RedisAutoConfiguration;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;

/**
 * 幂等功能配置
 *
 * @author wesley.zhang
 */
//@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class IdempotentAutoConfiguration {

    @Bean
    public RepeatSubmitAspect repeatSubmitAspect(RedisService redisService) {
        return new RepeatSubmitAspect(redisService);
    }

}
