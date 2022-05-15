package com.jbm.cluster.platform.gateway.utils;

import com.alibaba.fastjson.JSON;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;


/**
 * WebFlux 工具类
 *
 * @author Lion Li
 */
public class WebFluxUtils {
    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param msg      提示信息
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, String msg) {
        return webFluxResponseWriter(response, HttpStatus.OK, msg, null, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param e        错误信息
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, Exception e) {
        return webFluxResponseWriter(response, HttpStatus.OK, e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param value    响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, String msg, Object value) {
        return webFluxResponseWriter(response, HttpStatus.OK, msg, value, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param code     响应状态码
     * @param value    响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, String msg, Object value, int code) {
        return webFluxResponseWriter(response, HttpStatus.OK, msg, value, code);
    }

    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param status   http状态码
     * @param code     响应状态码
     * @param value    响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, HttpStatus status, String msg, Object value, int code) {
        return webFluxResponseWriter(response, MediaType.APPLICATION_JSON_VALUE, status, msg, value, code);
    }

    /**
     * 设置webflux模型响应
     *
     * @param response    ServerHttpResponse
     * @param contentType content-type
     * @param status      http状态码
     * @param code        响应状态码
     * @param value       响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, String contentType, HttpStatus status, String msg, Object value, int code) {
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
        ResultBody<?> result = ResultBody.failed().code(code).data(value).msg(msg);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JSON.toJSONBytes(result));
        return response.writeWith(Mono.just(dataBuffer));
    }
}
