package com.jbm.cluster.common.feign;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置注册
 *
 * @author wesley.zhang
 **/
@Configuration
public class FeignAutoConfiguration {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new FeignRequestInterceptor();
    }

    @Bean
    public AppPreRequestInterceptor appPreRequestInterceptor() {
        return new AppPreRequestInterceptor();
    }

    @Bean
    public FeignUnknownRuntimeExceptionFilter feginUnknownRuntimeExceptionFilter() {
        return new FeignUnknownRuntimeExceptionFilter();
    }
}
