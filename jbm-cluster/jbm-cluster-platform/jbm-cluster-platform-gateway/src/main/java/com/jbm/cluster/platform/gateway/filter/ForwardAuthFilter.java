package com.jbm.cluster.platform.gateway.filter;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.cluster.platform.gateway.filter.context.GatewayContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 转发认证过滤器：为下游内部服务注入 OAuth2 ClientToken 与调用方身份 Header。
 */
@Component
public class ForwardAuthFilter implements GlobalFilter {

    private static final String ORIGINAL_PATH_HEADER = "X-Original-Path";

    @Autowired
    private SaOAuth2Template saOAuth2Template;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String originalPath = exchange.getRequest().getURI().getPath();
        ClientTokenModel clientToken = saOAuth2Template.generateClientToken(SpringUtil.getApplicationName(), "*");
        String prefix = StrUtil.emptyToDefault(SaManager.getConfig().getTokenPrefix(), "Bearer");

        ServerHttpRequest newRequest = exchange.getRequest().mutate()
                .header(JbmSecurityConstants.AUTHORIZATION_HEADER, prefix + " " + clientToken.clientToken)
                .header(JbmSecurityConstants.INTERNAL_SERVICE, SpringUtil.getApplicationName())
                .header(JbmSecurityConstants.INTERNAL_INSTANCE,
                        SpringUtil.getApplicationName() + ":" + SpringUtil.getProperty("server.port", "0"))
                .header(ORIGINAL_PATH_HEADER, originalPath)
                .build();

        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
        newExchange.getAttributes().put(GatewayContext.REQUEST_TIME_HEAD, DateUtil.now());
        return chain.filter(newExchange);
    }
}
