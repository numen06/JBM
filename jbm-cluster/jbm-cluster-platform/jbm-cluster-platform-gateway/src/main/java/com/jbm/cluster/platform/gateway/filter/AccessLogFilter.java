package com.jbm.cluster.platform.gateway.filter;

import com.jbm.cluster.platform.gateway.resolver.DatabaseMessageSource;
import com.jbm.cluster.platform.gateway.service.AccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.io.Charsets;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;


/**
 * 日志过滤器
 *
 * @author wesley.zhang
 */
@Slf4j
@Component
public class AccessLogFilter implements WebFilter {

    private final AccessLogService accessLogService;

    @Qualifier("databaseMessageSource")
    @Autowired
    private DatabaseMessageSource messageSource;

    public AccessLogFilter(AccessLogService accessLogService) {
        this.accessLogService = accessLogService;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        DataBufferFactory bufferFactory = response.bufferFactory();
        StringBuffer responseBodys = new StringBuffer();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(response) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.map(dataBuffer -> {
                        // probably should reuse buffers
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        //释放掉内存
                        DataBufferUtils.release(dataBuffer);
                        responseBodys.append(getResponseBody(response, content));
                        return bufferFactory.wrap(content);
                    }));
                }
                // if body is not a flux. never got there.
                return super.writeWith(body);
            }
        };
        return chain.filter(exchange.mutate().response(decoratedResponse).build()).then(Mono.fromRunnable(() -> {
            accessLogService.sendLog(exchange, responseBodys.toString(), null);
        }));
    }

    private String getResponseBody(ServerHttpResponse response, byte[] content) {
        if (response.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE)) {
            if (StrUtil.contains(response.getHeaders().get(HttpHeaders.CONTENT_TYPE).toString(), MediaType.APPLICATION_JSON_VALUE)) {
                return StrUtil.str(content, Charsets.UTF_8);
            }
        }
        return null;
    }

}

