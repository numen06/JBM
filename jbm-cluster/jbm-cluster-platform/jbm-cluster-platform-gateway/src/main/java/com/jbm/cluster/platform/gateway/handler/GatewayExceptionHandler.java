package com.jbm.cluster.platform.gateway.handler;

import com.alibaba.fastjson.JSON;
import com.jbm.cluster.platform.gateway.service.AccessLogService;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
    @Autowired
    private AccessLogService accessLogService;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        ServerHttpRequest request = exchange.getRequest();
        String requestPath = exchange.getRequest().getURI().getPath();
        if ("/favicon.ico".equals(requestPath)) {
            return Mono.empty();
        }
        ResultBody resultBody;
        if (ex instanceof NotFoundException) {
            resultBody = webExceptionResolve.buildBody(ex, ErrorCode.SERVICE_UNAVAILABLE, requestPath, HttpStatus.SERVICE_UNAVAILABLE.value());
        } else {
            resultBody = webExceptionResolve.resolveException(ex, requestPath);
        }
        /**
         * 参考AbstractErrorWebExceptionHandler
         */
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        boolean client4xx = ex instanceof ResponseStatusException
                && ((ResponseStatusException) ex).getStatus().is4xxClientError();
        if (!client4xx && resultBody.getHttpStatus() != null) {
            try {
                client4xx = HttpStatus.valueOf(resultBody.getHttpStatus()).is4xxClientError();
            } catch (IllegalArgumentException ignored) {
                // ignore
            }
        }
        boolean routineAccessException = client4xx || ex instanceof NotFoundException;
        if (routineAccessException) {
            // 4xx + 网关找不到服务实例(503)：常见访问类异常，只记摘要
            log.warn("[网关异常处理]请求路径:{},异常信息:{}", exchange.getRequest().getPath(), ex.getMessage());
            // 含 404 No matching handler：凡经异常处理都要 sendLog，与「打到端口即有日志」一致
            Schedulers.boundedElastic().schedule(() -> accessLogService.sendLog(exchange, ex));
        } else {
            // 5xx、未知异常等：保留完整堆栈便于排障
            log.error("[网关异常处理]请求路径:{},异常信息:{}", exchange.getRequest().getPath(), ex.getMessage(), ex);
            Schedulers.boundedElastic().schedule(() -> accessLogService.sendLog(exchange, ex));
        }
        Integer resolvedHttpStatus = resultBody.getHttpStatus();
        if (resolvedHttpStatus == null || resolvedHttpStatus == 200) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            try {
                response.setStatusCode(HttpStatus.valueOf(resolvedHttpStatus));
            } catch (IllegalArgumentException ignored) {
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JSON.toJSONBytes(resultBody));
        return response.writeWith(Mono.just(dataBuffer));
    }

}