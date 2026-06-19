package com.jbm.cluster.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.cluster.core.security.InternalServiceTokenProvider;
import com.jbm.cluster.platform.gateway.filter.context.GatewayContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 转发认证过滤器：保留用户 Authorization，追加 OAuth2 Client Credentials JWT 与内部调用方身份 Header。
 */
@Component
public class ForwardAuthFilter implements GlobalFilter, Ordered {

    private static final String ORIGINAL_PATH_HEADER = "X-Original-Path";

    @Override
    public int getOrder() {
        return -40;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String originalPath = exchange.getRequest().getURI().getPath();

        ServerHttpRequest.Builder builder = exchange.getRequest().mutate()
                .header(JbmSecurityConstants.INTERNAL_SERVICE, SpringUtil.getApplicationName())
                .header(JbmSecurityConstants.INTERNAL_INSTANCE,
                        SpringUtil.getApplicationName() + ":" + SpringUtil.getProperty("server.port", "0"))
                .header(ORIGINAL_PATH_HEADER, originalPath);
        String authorization = InternalServiceTokenProvider.authorizationHeader();
        if (StrUtil.isNotBlank(authorization)) {
            builder.header(JbmSecurityConstants.INTERNAL_AUTHORIZATION_HEADER, authorization);
        }
        Object apiKeyId = exchange.getAttribute("gateway.apiKeyId");
        if (apiKeyId != null) {
            builder.header(JbmSecurityConstants.GATEWAY_API_KEY_ID, String.valueOf(apiKeyId));
        }
        ServerHttpRequest newRequest = builder.build();

        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
        newExchange.getAttributes().put(GatewayContext.REQUEST_TIME_HEAD, DateUtil.now());
        return chain.filter(newExchange);
    }

}
