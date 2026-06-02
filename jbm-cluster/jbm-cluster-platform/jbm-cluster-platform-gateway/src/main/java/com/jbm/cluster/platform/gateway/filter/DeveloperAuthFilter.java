package com.jbm.cluster.platform.gateway.filter;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import com.jbm.cluster.core.constant.ApiKeyConstants;
import com.jbm.cluster.core.constant.ApiSecurityConstants;
import com.jbm.cluster.common.mysql.service.BaseApiKeyService;
import com.jbm.cluster.common.mysql.service.BaseApiService;
import com.jbm.cluster.platform.gateway.config.properties.ApiSecurityProperties;
import com.jbm.cluster.platform.gateway.security.ApiClientConfigProvider;
import com.jbm.cluster.platform.gateway.utils.PathMatcherUtils;
import com.jbm.framework.exceptions.OpenSignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 第三方 API Key 授权校验：仅当 X-App-Id 对应 base_api_key 时生效
 */
@Slf4j
@Component
public class DeveloperAuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private ApiSecurityProperties apiSecurityProperties;
    @Autowired
    private ApiClientConfigProvider apiClientConfigProvider;
    @Autowired
    private BaseApiService baseApiService;
    @Autowired
    private BaseApiKeyService baseApiKeyService;

    private final LoadingCache<String, Boolean> authorityCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build(key -> {
                String[] parts = key.split(":", 2);
                if (parts.length != 2) {
                    return false;
                }
                return baseApiKeyService.hasAuthorityForApi(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
            });

    @Override
    public int getOrder() {
        return -45;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!apiSecurityProperties.isCheckAuth()) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (PathMatcherUtils.matches(path, apiSecurityProperties.getAuthIgnores())) {
            return chain.filter(exchange);
        }
        String appIdHeader = header(request, ApiSecurityConstants.APP_ID);
        if (StrUtil.isBlank(appIdHeader)) {
            return chain.filter(exchange);
        }
        BaseApiKey apiKeyRow = apiClientConfigProvider.resolveApiKey(appIdHeader);
        if (apiKeyRow == null) {
            return chain.filter(exchange);
        }
        if (apiKeyRow.getStatus() == null || apiKeyRow.getStatus() != ApiKeyConstants.API_KEY_STATUS_ENABLED) {
            return Mono.error(new OpenSignatureException("API Key 已禁用"));
        }
        if (apiKeyRow.getExpireTime() != null && apiKeyRow.getExpireTime().before(new Date())) {
            return Mono.error(new OpenSignatureException("API Key 已过期"));
        }
        exchange.getAttributes().put("gateway.apiKeyId", apiKeyRow.getKeyId());
        exchange.getAttributes().put("gateway.apiKeyName", apiKeyRow.getKeyName());
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null || route.getUri() == null) {
            return chain.filter(exchange);
        }
        String serviceId = resolveServiceId(route);
        String realPath = normalizePath(path);
        BaseApi baseApi;
        try {
            baseApi = baseApiService.findApiByPath(serviceId, realPath);
        } catch (Exception e) {
            log.debug("[DeveloperAuthFilter] findApiByPath failed: {}", e.getMessage());
            return chain.filter(exchange);
        }
        if (ObjectUtil.isEmpty(baseApi) || baseApi.getApiId() == null) {
            return chain.filter(exchange);
        }
        String cacheKey = apiKeyRow.getKeyId() + ":" + baseApi.getApiId();
        boolean allowed;
        try {
            allowed = Boolean.TRUE.equals(authorityCache.get(cacheKey));
        } catch (Exception e) {
            log.warn("[DeveloperAuthFilter] authority check failed keyId={} apiId={}", apiKeyRow.getKeyId(), baseApi.getApiId());
            return Mono.error(new OpenSignatureException("授权校验失败"));
        }
        if (!allowed) {
            return Mono.error(new OpenSignatureException("超出 API Key 授权范围"));
        }
        return chain.filter(exchange);
    }

    private static String normalizePath(String requestPath) {
        if (StrUtil.isBlank(requestPath)) {
            return "/";
        }
        return requestPath.startsWith("/") ? requestPath : "/" + requestPath;
    }

    private static String resolveServiceId(Route route) {
        if (route.getMetadata() != null) {
            Object meta = route.getMetadata().get("serviceId");
            if (meta != null && StrUtil.isNotBlank(String.valueOf(meta))) {
                return String.valueOf(meta).trim();
            }
        }
        return route.getUri().getAuthority();
    }

    private static String header(ServerHttpRequest request, String name) {
        return request.getHeaders().getFirst(name);
    }
}
