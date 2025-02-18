package com.jbm.cluster.platform.gateway.filter;

import com.jbm.cluster.platform.gateway.resolver.DatabaseMessageSource;
import com.jbm.cluster.platform.gateway.service.AccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * 日志过滤器
 *
 * @author wesley.zhang
 */
@Slf4j
@Component
public class AccessLogFilter implements WebFilter {

    private final AccessLogService accessLogService;

    @Resource(name = "databaseMessageSource")
    private DatabaseMessageSource messageSource;

    public AccessLogFilter(AccessLogService accessLogService) {
        this.accessLogService = accessLogService;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return chain.filter(exchange);
        }
        if (response.getStatusCode().isError()) {
            return chain.filter(exchange);
        }
//        DataBufferFactory bufferFactory = response.bufferFactory();
        StringBuffer responseBodys = new StringBuffer();
        ServerHttpResponseDecorator decoratedResponse = new FluxHttpResponseDecorator(response, exchange, responseBodys, messageSource);
        return chain.filter(exchange.mutate().response(decoratedResponse).build()).then(Mono.fromRunnable(() -> {
            accessLogService.sendLog(exchange, responseBodys.toString(), null);
        }));
    }


}

