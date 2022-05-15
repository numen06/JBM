package com.jbm.cluster.platform.gateway.config;

import com.jbm.cluster.platform.gateway.filter.GatewayContextFilter;
import com.jbm.cluster.platform.gateway.filter.RemoveGatewayContextFilter;
import com.jbm.cluster.platform.gateway.handler.SentinelFallbackHandler;
import com.jbm.cluster.platform.gateway.handler.WebExceptionResolve;
import com.jbm.cluster.platform.gateway.locator.DynamicResourceLocator;
import com.jbm.cluster.platform.gateway.locator.DynamicRouteDefinitionLocator;
import com.jbm.cluster.platform.gateway.service.RouteDataSource;
import com.jbm.cluster.platform.gateway.service.impl.JdbcRouteDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.InMemoryRouteDefinitionRepository;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 网关限流配置
 *
 * @author wesley.zhang
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({JdbcDataSourceProperties.class})
public class GatewayConfig {


    /**
     * Sentinel负载均衡器
     *
     * @return
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelFallbackHandler sentinelGatewayExceptionHandler() {
        return new SentinelFallbackHandler();
    }


    /**
     * 从数据库加载路由
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(RouteDataSource.class)
    public JdbcRouteDataSource jdbcRouteDataSource(JdbcDataSourceProperties dataSourceProperties) {
        return new JdbcRouteDataSource(dataSourceProperties);
    }

    /**
     * 动态路由加载
     *
     * @return
     */
    @Bean
    public DynamicRouteDefinitionLocator dynamicRouteDefinitionLocator(RouteDataSource routeDataSource, InMemoryRouteDefinitionRepository repository) {
        DynamicRouteDefinitionLocator jdbcRouteDefinitionLocator = new DynamicRouteDefinitionLocator(routeDataSource, repository);
        log.info("JdbcRouteDefinitionLocator [{}]", jdbcRouteDefinitionLocator);
        return jdbcRouteDefinitionLocator;
    }

    /**
     * 动态路由加载
     *
     * @return
     */
    @Bean
    @Lazy
    public DynamicResourceLocator resourceLocator(RouteDefinitionLocator routeDefinitionLocator) {
        DynamicResourceLocator resourceLocator = new DynamicResourceLocator(routeDefinitionLocator);
        log.info("ResourceLocator [{}]", resourceLocator);
        return resourceLocator;
    }

    @Bean
    @ConditionalOnMissingBean(GatewayContextFilter.class)
    public GatewayContextFilter gatewayContextFilter() {
        log.info("Load GatewayContextFilter Config Bean");
        return new GatewayContextFilter();
    }


    @Bean
    @ConditionalOnMissingBean(RemoveGatewayContextFilter.class)
    public RemoveGatewayContextFilter removeGatewayContextFilter() {
        RemoveGatewayContextFilter gatewayContextFilter = new RemoveGatewayContextFilter();
        log.info("Load RemoveGatewayContextFilter Config Bean");
        return gatewayContextFilter;
    }

    @Bean
    public WebExceptionResolve webExceptionResolve(MessageSource messageSource) {
        return new WebExceptionResolve(messageSource);
    }

    @Bean
    @Primary
    public I18nConfig i18nConfig() {
        return new I18nConfig();
    }


}