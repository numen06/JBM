package com.jbm.cluster.platform.gateway.filter;

import cn.dev33.satoken.id.SaIdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.cluster.platform.gateway.filter.context.GatewayContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 转发认证过滤器：保留用户 Authorization，追加 Id-Token 与内部调用方身份 Header。
 */
@Component
public class ForwardAuthFilter implements GlobalFilter {

    private static final String ORIGINAL_PATH_HEADER = "X-Original-Path";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String originalPath = exchange.getRequest().getURI().getPath();

        ServerHttpRequest newRequest = exchange.getRequest().mutate()
                .header(SaIdUtil.ID_TOKEN, idToken())
                .header(JbmSecurityConstants.INTERNAL_SERVICE, SpringUtil.getApplicationName())
                .header(JbmSecurityConstants.INTERNAL_INSTANCE,
                        SpringUtil.getApplicationName() + ":" + SpringUtil.getProperty("server.port", "0"))
                .header(ORIGINAL_PATH_HEADER, originalPath)
                .build();

        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
        newExchange.getAttributes().put(GatewayContext.REQUEST_TIME_HEAD, DateUtil.now());
        return chain.filter(newExchange);
    }

    private static String idToken() {
        String token = SaIdUtil.getToken();
        if (StrUtil.isBlank(token)) {
            token = SaIdUtil.refreshToken();
        }
        return token;
    }
}
