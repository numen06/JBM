package com.jbm.cluster.platform.gateway.filter;

import cn.dev33.satoken.id.SaIdUtil;
import cn.hutool.core.date.DateUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.cluster.platform.gateway.filter.context.GatewayContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 转发认证过滤器(内部服务外网隔离)
 *
 * @author Lion Li
 */
@Component
public class ForwardAuthFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String originalPath = exchange.getRequest().getURI().getPath();
        ServerHttpRequest newRequest = exchange
                .getRequest()
                .mutate()
                .headers(headers -> {
                    headers.set(SaIdUtil.ID_TOKEN, getIdToken());
                    headers.remove(JbmSecurityConstants.FROM_SOURCE);
                    headers.set("X-Original-Path", originalPath);
                })
                .build();
        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
        // 添加请求时间
        newExchange.getAttributes().put(GatewayContext.REQUEST_TIME_HEAD, DateUtil.now());
        return chain.filter(newExchange);
    }

    String getIdToken() {
        return SaIdUtil.getToken();
    }
}
