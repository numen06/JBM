package com.jbm.cluster.platform.gateway.locator;

import com.google.common.collect.Lists;
import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.cluster.api.model.RateLimitApi;
import com.jbm.cluster.common.mysql.service.GatewayRateLimitService;
import com.jbm.cluster.common.mysql.service.GatewayRouteService;
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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义动态路由加载器
 *
 * @author wesley.zhang
 */
@Slf4j
public class DynamicRouteDefinitionLocator extends DynamicResourceService implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher publisher;
    private InMemoryRouteDefinitionRepository repository;
    private GatewayRouteService gatewayRouteService;
    private GatewayRateLimitService gatewayRateLimitService;
    private final Set<String> managedRouteIds = ConcurrentHashMap.newKeySet();

    public DynamicRouteDefinitionLocator(
            GatewayRouteService gatewayRouteService,
            GatewayRateLimitService gatewayRateLimitService,
            InMemoryRouteDefinitionRepository repository) {
        this.gatewayRouteService = gatewayRouteService;
        this.gatewayRateLimitService = gatewayRateLimitService;
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

    private Integer getStripPrefix(List<GatewayRoute> routeList, String serviceId) {
        if (routeList != null) {
            for (GatewayRoute route : routeList) {
                if (route.getServiceId() != null && route.getServiceId().equals(serviceId)) {
                    return route.getStripPrefix();
                }
            }
        }
        return 1;
    }

    private void addStripPrefixFilter(List<FilterDefinition> filters, Integer stripPrefix) {
        int stripPrefixValue = stripPrefix == null ? 1 : stripPrefix;
        if (stripPrefixValue <= 0) {
            return;
        }
        FilterDefinition stripPrefixDefinition = new FilterDefinition();
        Map<String, String> stripPrefixParams = new HashMap<>(8);
        stripPrefixDefinition.setName("StripPrefix");
        stripPrefixParams.put(NameUtils.GENERATED_NAME_PREFIX + "0", String.valueOf(stripPrefixValue));
        stripPrefixDefinition.setArgs(stripPrefixParams);
        filters.add(stripPrefixDefinition);
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
            clearManagedRoutes();
            List<GatewayRoute> loadedRoutes = gatewayRouteService.findRouteList();
            List<RateLimitApi> loadedLimitApis = gatewayRateLimitService.findRateLimitApiList();
            final List<GatewayRoute> routeList = loadedRoutes == null ? Collections.emptyList() : loadedRoutes;
            final List<RateLimitApi> limitApiList = loadedLimitApis == null ? Collections.emptyList() : loadedLimitApis;
            if (!limitApiList.isEmpty()) {
                // 加载限流
                limitApiList.forEach(item -> {
                    long[] arry = DynamicResourceLocator.getIntervalAndQuota(item.getIntervalUnit());
                    Long refreshInterval = arry[0];
                    Long quota = arry[1];
                    // 允许用户每秒处理多少个请求
                    long replenishRate = item.getLimitQuota() / refreshInterval;
                    replenishRate = Math.max(replenishRate, 1);
                    // 令牌桶的容量，允许在一秒钟内完成的最大请求数
                    long burstCapacity = replenishRate * 2;
                    RouteDefinition definition = new RouteDefinition();
                    List<PredicateDefinition> predicates = Lists.newArrayList();
                    List<FilterDefinition> filters = Lists.newArrayList();
                    definition.setId(item.getApiId().toString());
                    managedRouteIds.add(definition.getId());
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
                    addStripPrefixFilter(filters, getStripPrefix(routeList, item.getServiceId()));
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
            if (!routeList.isEmpty()) {
                // 最后加载路由
                routeList.forEach(gatewayRoute -> {
                    try {
                        RouteDefinition definition = new RouteDefinition();
                        List<PredicateDefinition> predicates = Lists.newArrayList();
                        List<FilterDefinition> filters = Lists.newArrayList();
                        definition.setId(gatewayRoute.getRouteName());
                        managedRouteIds.add(definition.getId());
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

                        addStripPrefixFilter(filters, gatewayRoute.getStripPrefix());

                        definition.setPredicates(predicates);
                        definition.setFilters(filters);
                        definition.setUri(uri);
                        this.repository.save(Mono.just(definition)).subscribe();
                    } catch (Exception e) {
                        log.error("加载动态路由错误,跳过当前路由:{}", gatewayRoute.getPath(), e);
                    }
                });
            }
            log.info("=============加载动态路由:{}==============", routeList.size());
            log.info("=============加载动态限流:{}==============", limitApiList.size());
        } catch (Exception e) {
            log.error("加载动态路由错误", e);
        }
        return Mono.empty();
    }

    private void clearManagedRoutes() {
        managedRouteIds.forEach(routeId -> repository.delete(Mono.just(routeId))
                .onErrorResume(e -> Mono.empty())
                .subscribe());
        managedRouteIds.clear();
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }
}
