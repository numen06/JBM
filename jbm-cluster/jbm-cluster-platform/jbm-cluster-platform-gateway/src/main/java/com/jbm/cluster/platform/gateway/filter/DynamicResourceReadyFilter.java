package com.jbm.cluster.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.platform.gateway.locator.DynamicResourceLocator;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 动态资源未加载完成前，网关不对业务流量提供服务。
 */
public class DynamicResourceReadyFilter implements GlobalFilter, Ordered {

    private final DynamicResourceLocator resourceLocator;

    public DynamicResourceReadyFilter(DynamicResourceLocator resourceLocator) {
        this.resourceLocator = resourceLocator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (resourceLocator.isReady() || isHealthPath(path)) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        ResultBody resultBody = ResultBody.failed()
                .code(HttpStatus.SERVICE_UNAVAILABLE.value())
                .msg("网关动态资源加载中，请稍后重试")
                .path(path)
                .httpStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        DataBuffer dataBuffer = exchange.getResponse().bufferFactory().wrap(JSON.toJSONBytes(resultBody));
        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

    private boolean isHealthPath(String path) {
        return StrUtil.equals(path, "/favicon.ico")
                || StrUtil.equals(path, "/static/favicon.ico")
                || StrUtil.startWith(path, "/actuator/health")
                || StrUtil.startWith(path, "/actuator/info");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
