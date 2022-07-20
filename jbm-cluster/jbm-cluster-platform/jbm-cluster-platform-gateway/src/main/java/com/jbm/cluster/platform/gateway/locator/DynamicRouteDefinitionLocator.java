package com.jbm.cluster.platform.gateway.locator;

import com.google.common.collect.Lists;
import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.cluster.api.model.RateLimitApi;
import com.jbm.cluster.platform.gateway.service.RouteDataSource;
import com.jbm.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.InMemoryRouteDefinitionRepository;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义动态路由加载器
 *
 * @author wesley.zhang
 */
@Slf4j
public class DynamicRouteDefinitionLocator extends DynamicResourceService implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher publisher;
    private InMemoryRouteDefinitionRepository repository;
    private RouteDataSource routeDataSource;

    public DynamicRouteDefinitionLocator(RouteDataSource routeDataSource, InMemoryRouteDefinitionRepository repository) {
        this.routeDataSource = routeDataSource;
        this.repository = repository;
    }


    /**
     * 刷新路由
     *
     * @return
     */
    public void refresh() {
        this.loadRoutes();
        // 触发默认路由刷新事件,刷新缓存路由
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }


    protected String getFullPath(List<GatewayRoute> routeList, String serviceId, String path) {
        final String[] fullPath = {path.startsWith("/") ? path : "/" + path};
        if (routeList != null) {
            routeList.forEach(route -> {
                if (route.getServiceId() != null && route.getServiceId().equals(serviceId)) {
                    fullPath[0] = route.getPath().replace("/**", path.startsWith("/") ? path : "/" + path);
                    return;
                }
            });
        }
        return fullPath[0];
    }

    /**
     * 动态加载路由
     * * 示例
     * id: opencloud-admin-provider
     * uri: lb://opencloud-admin-provider
     * predicates:
     * - Path=/admin/**
     * - Name=平台后台管理服务
     * filters:
     * #转发去掉前缀,总要否则swagger无法加载
     * - StripPrefix=1
     *
     * @return
     */
    private Mono<Void> loadRoutes() {
        //从数据库拿到路由配置
        try {
            List<GatewayRoute> routeList = routeDataSource.getRouteList();
            List<RateLimitApi> limitApiList = routeDataSource.getLimitApiList();
            if (limitApiList != null) {
                // 加载限流
                limitApiList.forEach(item -> {
                    long[] arry = DynamicResourceLocator.getIntervalAndQuota(item.getIntervalUnit());
                    Long refreshInterval = arry[0];
                    Long quota = arry[1];
                    // 允许用户每秒处理多少个请求
                    long replenishRate = item.getLimitQuota() / refreshInterval;
                    replenishRate = replenishRate < 1 ? 1 : refreshInterval;
                    // 令牌桶的容量，允许在一秒钟内完成的最大请求数
                    long burstCapacity = replenishRate * 2;
                    RouteDefinition definition = new RouteDefinition();
                    List<PredicateDefinition> predicates = Lists.newArrayList();
                    List<FilterDefinition> filters = Lists.newArrayList();
                    definition.setId(item.getApiId().toString());
                    PredicateDefinition predicatePath = new PredicateDefinition();
                    String fullPath = getFullPath(routeList, item.getServiceId(), item.getPath());
                    Map<String, String> predicatePathParams = new HashMap<>(8);
                    predicatePath.setName("Path");
                    predicatePathParams.put("pattern", fullPath);
                    predicatePathParams.put("pathPattern", fullPath);
                    predicatePathParams.put("_rateLimit", "1");
                    predicatePath.setArgs(predicatePathParams);
                    predicates.add(predicatePath);

                    // 服务地址
                    URI uri = UriComponentsBuilder.fromUriString(StringUtils.isNotBlank(item.getUrl()) ? item.getUrl() : "lb://" + item.getServiceId()).build().toUri();

                    // 路径去前缀
                    FilterDefinition stripPrefixDefinition = new FilterDefinition();
                    Map<String, String> stripPrefixParams = new HashMap<>(8);
                    stripPrefixDefinition.setName("StripPrefix");
                    stripPrefixParams.put(NameUtils.GENERATED_NAME_PREFIX + "0", "1");
                    stripPrefixDefinition.setArgs(stripPrefixParams);
                    filters.add(stripPrefixDefinition);
                    // 限流
                    FilterDefinition rateLimiterDefinition = new FilterDefinition();
                    Map<String, String> rateLimiterParams = new HashMap<>(8);
                    rateLimiterDefinition.setName("RequestRateLimiter");
                    //令牌桶流速
                    rateLimiterParams.put("redis-rate-limiter.replenishRate", String.valueOf(replenishRate));
                    //令牌桶容量
                    rateLimiterParams.put("redis-rate-limiter.burstCapacity", String.valueOf(burstCapacity));
                    // 限流策略(#{@BeanName})
                    rateLimiterParams.put("key-resolver", "#{@pathKeyResolver}");
                    rateLimiterDefinition.setArgs(rateLimiterParams);
                    filters.add(rateLimiterDefinition);

                    definition.setPredicates(predicates);
                    definition.setFilters(filters);
                    definition.setUri(uri);
                    this.repository.save(Mono.just(definition)).subscribe();
                });
            }
            if (routeList != null) {
                // 最后加载路由
                routeList.forEach(gatewayRoute -> {
                    RouteDefinition definition = new RouteDefinition();
                    List<PredicateDefinition> predicates = Lists.newArrayList();
                    List<FilterDefinition> filters = Lists.newArrayList();
                    definition.setId(gatewayRoute.getRouteName());
                    // 路由地址
                    PredicateDefinition predicatePath = new PredicateDefinition();
                    Map<String, String> predicatePathParams = new HashMap<>(8);
                    predicatePath.setName("Path");
                    predicatePathParams.put("name", StringUtils.isBlank(gatewayRoute.getRouteName()) ? gatewayRoute.getRouteId().toString() : gatewayRoute.getRouteName());
                    predicatePathParams.put("pattern", gatewayRoute.getPath());
                    predicatePathParams.put("pathPattern", gatewayRoute.getPath());
                    predicatePath.setArgs(predicatePathParams);
                    predicates.add(predicatePath);
                    // 服务地址
                    URI uri = UriComponentsBuilder.fromUriString(StringUtils.isNotBlank(gatewayRoute.getUrl()) ? gatewayRoute.getUrl() : "lb://" + gatewayRoute.getServiceId()).build().toUri();

                    FilterDefinition stripPrefixDefinition = new FilterDefinition();
                    Map<String, String> stripPrefixParams = new HashMap<>(8);
                    stripPrefixDefinition.setName("StripPrefix");
                    stripPrefixParams.put(NameUtils.GENERATED_NAME_PREFIX + "0", "1");
                    stripPrefixDefinition.setArgs(stripPrefixParams);
                    filters.add(stripPrefixDefinition);

                    definition.setPredicates(predicates);
                    definition.setFilters(filters);
                    definition.setUri(uri);
                    this.repository.save(Mono.just(definition)).subscribe();
                });
            }
            log.info("=============加载动态路由:{}==============", routeList.size());
            log.info("=============加载动态限流:{}==============", limitApiList.size());
        } catch (Exception e) {
            log.error("加载动态路由错误:{}", e);
        }
        return Mono.empty();
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }
}
