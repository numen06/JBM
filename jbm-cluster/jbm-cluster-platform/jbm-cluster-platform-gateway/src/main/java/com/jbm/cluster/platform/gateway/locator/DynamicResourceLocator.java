package com.jbm.cluster.platform.gateway.locator;

import com.google.common.collect.Lists;
import com.jbm.cluster.api.entitys.auth.AuthorityResource;
import com.jbm.cluster.api.model.IpLimitApi;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.mysql.service.GatewayIpLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 动态资源加载器
 *
 * @author wesley.zhang
 */
@Slf4j
public class DynamicResourceLocator extends DynamicResourceService {

    /**
     * 权限资源
     */
    private List<AuthorityResource> authorityResources;

    /**
     * ip黑名单
     */
    private List<IpLimitApi> ipBlacks;

    /**
     * ip白名单
     */
    private List<IpLimitApi> ipWhites;

    /**
     * 缓存
     */
    private Map<String, Object> cache = new ConcurrentHashMap<>();

    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private GatewayIpLimitService gatewayIpLimitService;

    private RouteDefinitionLocator routeDefinitionLocator;

    public DynamicResourceLocator() {
        authorityResources = new CopyOnWriteArrayList<>();
        ipBlacks = new CopyOnWriteArrayList<>();
        ipWhites = new CopyOnWriteArrayList<>();
    }

    public DynamicResourceLocator(RouteDefinitionLocator routeDefinitionLocator) {
        this();
        this.routeDefinitionLocator = routeDefinitionLocator;
    }

    /**
     * 清空缓存并刷新
     */
    public void refresh() {
        this.cache.clear();
        this.authorityResources = loadAuthorityResources();
        this.ipBlacks = loadIpBlackList();
        this.ipWhites = loadIpWhiteList();
    }

    /**
     * 获取路由后的地址
     */
    protected String getFullPath(String serviceId, String path) {
        return getFullPath(loadRoutePathPatterns(), serviceId, path);
    }

    private Map<String, String> loadRoutePathPatterns() {
        Map<String, String> patterns = new HashMap<>(16);
        if (routeDefinitionLocator == null) {
            return patterns;
        }
        try {
            List<RouteDefinition> routeDefinitions = routeDefinitionLocator.getRouteDefinitions()
                    .collectList()
                    .block(Duration.ofSeconds(10));
            if (routeDefinitions == null) {
                return patterns;
            }
            routeDefinitions.forEach(routeDefinition -> routeDefinition.getPredicates().stream()
                            .filter(predicateDefinition -> ("Path").equalsIgnoreCase(predicateDefinition.getName()))
                            .filter(predicateDefinition -> !predicateDefinition.getArgs().containsKey("_rateLimit"))
                            .findFirst()
                            .map(this::getPathPattern)
                            .ifPresent(pattern -> patterns.put(routeDefinition.getId(), pattern)));
        } catch (Exception e) {
            log.warn("加载动态路由路径缓存失败，将使用资源原始路径", e);
        }
        return patterns;
    }

    private String getPathPattern(PredicateDefinition predicateDefinition) {
        String pattern = predicateDefinition.getArgs().get("pattern");
        if (pattern == null && !predicateDefinition.getArgs().isEmpty()) {
            pattern = predicateDefinition.getArgs().values().iterator().next();
        }
        return pattern;
    }

    protected String getFullPath(Map<String, String> routePathPatterns, String serviceId, String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        if (serviceId == null || routePathPatterns == null) {
            return normalizedPath;
        }
        String pattern = routePathPatterns.get(serviceId);
        if (pattern == null) {
            return normalizedPath;
        }
        return pattern.replace("/**", normalizedPath);
    }

    /**
     * 加载授权列表
     */
    public List<AuthorityResource> loadAuthorityResources() {
        List<AuthorityResource> resources = Lists.newArrayList();
        try {
            resources = baseAuthorityService.findAuthorityResource();
            if (resources != null) {
                Map<String, String> routePathPatterns = loadRoutePathPatterns();
                for (AuthorityResource item : resources) {
                    String path = item.getPath();
                    if (path == null) {
                        continue;
                    }
                    String fullPath = getFullPath(routePathPatterns, item.getServiceId(), path);
                    item.setPath(fullPath);
                }
                log.info("=============加载动态权限:{}==============", resources.size());
            }
        } catch (Exception e) {
            log.error("加载动态权限错误", e);
        }
        return resources;
    }

    /**
     * 加载IP黑名单
     */
    public List<IpLimitApi> loadIpBlackList() {
        List<IpLimitApi> list = Lists.newArrayList();
        try {
            list = gatewayIpLimitService.findBlackList();
            if (list != null) {
                Map<String, String> routePathPatterns = loadRoutePathPatterns();
                for (IpLimitApi item : list) {
                    item.setPath(getFullPath(routePathPatterns, item.getServiceId(), item.getPath()));
                }
                log.info("=============加载IP黑名单:{}==============", list.size());
            }
        } catch (Exception e) {
            log.error("加载IP黑名单错误", e);
        }
        return list;
    }

    /**
     * 加载IP白名单
     */
    public List<IpLimitApi> loadIpWhiteList() {
        List<IpLimitApi> list = Lists.newArrayList();
        try {
            list = gatewayIpLimitService.findWhiteList();
            if (list != null) {
                Map<String, String> routePathPatterns = loadRoutePathPatterns();
                for (IpLimitApi item : list) {
                    item.setPath(getFullPath(routePathPatterns, item.getServiceId(), item.getPath()));
                }
                log.info("=============加载IP白名单:{}==============", list.size());
            }
        } catch (Exception e) {
            log.error("加载IP白名单错误", e);
        }
        return list;
    }
}
