package com.jbm.cluster.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.core.constant.ApiSecurityConstants;
import com.jbm.cluster.core.security.ApiSecurityUtils;
import com.jbm.cluster.platform.gateway.config.properties.ApiSecurityProperties;
import com.jbm.cluster.platform.gateway.filter.context.GatewayContext;
import com.jbm.cluster.platform.gateway.security.ApiClientConfigProvider;
import com.jbm.cluster.platform.gateway.utils.PathMatcherUtils;
import com.jbm.framework.exceptions.OpenSignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ApiSignatureFilter implements GlobalFilter, Ordered {

    @Autowired
    private ApiSecurityProperties apiSecurityProperties;

    @Autowired
    private ApiClientConfigProvider apiClientConfigProvider;

    @Override
    public int getOrder() {
        return -50;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!apiSecurityProperties.isCheckSign()) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (PathMatcherUtils.matches(path, apiSecurityProperties.getSignIgnores())) {
            return chain.filter(exchange);
        }
        String appId = header(request, ApiSecurityConstants.APP_ID);
        String timestamp = header(request, ApiSecurityConstants.TIMESTAMP);
        String signature = header(request, ApiSecurityConstants.SIGNATURE);
        if (StrUtil.hasBlank(appId, timestamp, signature)) {
            String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (StrUtil.isNotBlank(authorization) && StrUtil.startWithIgnoreCase(authorization.trim(), "Bearer ")) {
                // 管理端/开发者门户：password 登录后的 Bearer 会话，不要求网关签名
                return chain.filter(exchange);
            }
            return Mono.error(new OpenSignatureException("缺少签名参数"));
        }
        if (!ApiSecurityUtils.isTimestampValid(timestamp, apiSecurityProperties.getSignExpireMs())) {
            return Mono.error(new OpenSignatureException("请求已过期"));
        }
        String publicKey = apiClientConfigProvider.getPublicKey(appId);
        if (StrUtil.isBlank(publicKey)) {
            return Mono.error(new OpenSignatureException("无效的 AppId"));
        }
        GatewayContext context = exchange.getAttribute(GatewayContext.CACHE_GATEWAY_CONTEXT);
        String body = context != null ? StrUtil.nullToEmpty(context.getRequestBody()) : "";
        String signContent = ApiSecurityUtils.buildSignContent(request, body, timestamp, appId);
        if (!ApiSecurityUtils.verify(signContent, signature, publicKey)) {
            log.debug("[ApiSignatureFilter] verify failed path={} appId={}", path, appId);
            return Mono.error(new OpenSignatureException("签名验证失败"));
        }
        return chain.filter(exchange);
    }

    private static String header(ServerHttpRequest request, String name) {
        return request.getHeaders().getFirst(name);
    }
}
