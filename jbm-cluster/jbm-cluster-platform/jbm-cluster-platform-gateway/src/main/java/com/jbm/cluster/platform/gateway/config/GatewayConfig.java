package com.jbm.cluster.platform.gateway.config;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.jbm.cluster.platform.gateway.handler.SentinelFallbackHandler;
import com.jbm.cluster.platform.gateway.handler.WebExceptionResolve;
import com.jbm.cluster.platform.gateway.locator.DynamicResourceLocator;
import com.jbm.cluster.platform.gateway.locator.DynamicRouteDefinitionLocator;
import com.jbm.cluster.platform.gateway.resolver.DatabaseMessageSource;
import com.jbm.cluster.platform.gateway.resolver.I18nLocaleResolver;
import com.jbm.cluster.common.mysql.service.GatewayRateLimitService;
import com.jbm.cluster.common.mysql.service.GatewayRouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.InMemoryRouteDefinitionRepository;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.i18n.LocaleContextResolver;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;


/**
 * 网关限流配置
 *
 * @author wesley.zhang
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({JdbcDataSourceProperties.class})
public class GatewayConfig implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(org.springframework.http.codec.ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024);
    }

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
     * 动态路由加载
     *
     * @return
     */
    @Bean
    public DynamicRouteDefinitionLocator dynamicRouteDefinitionLocator(
            GatewayRouteService gatewayRouteService,
            GatewayRateLimitService gatewayRateLimitService,
            InMemoryRouteDefinitionRepository repository) {
        DynamicRouteDefinitionLocator locator = new DynamicRouteDefinitionLocator(gatewayRouteService, gatewayRateLimitService, repository);
        log.info("DynamicRouteDefinitionLocator [{}]", locator);
        return locator;
    }

    /**
     * 动态路由加载
     *
     * @return
     */
    @Bean
//    @Lazy
    public DynamicResourceLocator resourceLocator(RouteDefinitionLocator routeDefinitionLocator) {
        DynamicResourceLocator resourceLocator = new DynamicResourceLocator(routeDefinitionLocator);
        log.info("ResourceLocator [{}]", resourceLocator);
        return resourceLocator;
    }

    /**
     * 动态限流路由 RequestRateLimiter 使用的 key 解析器（按请求路径）。
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getURI().getPath());
    }

    @Bean
    public WebExceptionResolve webExceptionResolve(MessageSource messageSource) {
        return new WebExceptionResolve(messageSource);
    }

    @Bean
    @Primary
    public LocaleContextResolver localeContextResolver() {
        return new I18nLocaleResolver();
    }

    @Bean(name = "databaseMessageSource")
    public DatabaseMessageSource databaseMessageSource(JdbcDataSourceProperties jdbcDataSourceProperties) {
        return new DatabaseMessageSource(jdbcDataSourceProperties);
    }

    /** Gateway 为 Reactive 应用，勿注册 Servlet 版 HttpMessageConverters。 */
    @Bean
    public FastJsonHttpMessageConverter fastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
        return converter;
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
