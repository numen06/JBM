package com.jbm.micro.mysql.support;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 仅测试 classpath：在未装配 {@link PrometheusMeterRegistry} 时补一个默认实例，
 * 避免旧版传递依赖中 {@code MetricsConfiguration} 仍强依赖该 Bean 导致上下文无法启动。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(PrometheusMeterRegistry.class)
public class PrometheusMeterRegistryTestAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PrometheusMeterRegistry.class)
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
}
