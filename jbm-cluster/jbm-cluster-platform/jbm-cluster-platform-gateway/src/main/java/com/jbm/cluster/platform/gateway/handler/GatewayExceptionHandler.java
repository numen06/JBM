package com.jbm.cluster.platform.gateway.handler;

import com.alibaba.fastjson.JSON;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.metadata.enumerate.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关统一异常处理
 *
 * @author wesley.zhang
 */
@Slf4j
@Order(-1)
@Configuration
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    @Autowired
    private WebExceptionResolve webExceptionResolve;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        ResultBody resultBody;
        ServerHttpRequest request = exchange.getRequest();
        if ("/favicon.ico".equals(exchange.getRequest().getURI().getPath())) {
            return Mono.empty();
        }
        if (ex instanceof NotFoundException) {
            resultBody = ResultBody.failed().code(ErrorCode.SERVICE_UNAVAILABLE.getCode()).msg(ErrorCode.SERVICE_UNAVAILABLE.getMessage())
                    .httpStatus(HttpStatus.SERVICE_UNAVAILABLE.value()).path(request.getURI().getPath());
            log.error("==> 错误解析:{}", resultBody);
        } else {
            resultBody = webExceptionResolve.resolveException((Exception) ex, exchange.getRequest().getURI().getPath());
        }
        /**
         * 参考AbstractErrorWebExceptionHandler
         */
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        log.error("[网关异常处理]请求路径:{},异常信息:{}", exchange.getRequest().getPath(), ex.getMessage());
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JSON.toJSONBytes(resultBody));
        return response.writeWith(Mono.just(dataBuffer));
    }

}