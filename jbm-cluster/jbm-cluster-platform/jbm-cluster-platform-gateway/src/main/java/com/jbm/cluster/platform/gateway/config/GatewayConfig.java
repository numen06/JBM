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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.util.pattern.PathPatternParser;

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


    /**
     * 该访问配置跨域访问执行
     *
     * @return
     */
    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true); // 允许cookies跨域
        config.addAllowedOriginPattern("*");// #允许向该服务器提交请求的URI，*表示全部允许，在SpringMVC中，如果设成*，会自动转成当前请求头中的Origin
        config.addAllowedHeader("*");// #允许访问的头信息,*表示全部
        config.setMaxAge(18000L);// 预检请求的缓存时间（秒），即在这个时间段里，对于相同的跨域请求不会再预检了
        config.addAllowedMethod("OPTIONS");// 允许提交请求的方法类型，*表示全部允许
        config.addAllowedMethod("HEAD");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}