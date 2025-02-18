package com.jbm.cluster.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.jbm.cluster.platform.gateway.resolver.DatabaseMessageSource;
import org.apache.commons.io.Charsets;
import org.reactivestreams.Publisher;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author wesley
 */
public class FluxHttpResponseDecorator extends ServerHttpResponseDecorator {
    private final ServerWebExchange exchange;
    private final StringBuffer responseBodys;
    private final DatabaseMessageSource messageSource;

    public FluxHttpResponseDecorator(ServerHttpResponse response, ServerWebExchange exchange, StringBuffer responseBodys, DatabaseMessageSource messageSource) {
        super(response);
        this.exchange = exchange;
        this.responseBodys = responseBodys;
        this.messageSource = messageSource;
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        ServerHttpResponse response = exchange.getResponse();
        DataBufferFactory bufferFactory = response.bufferFactory();
        if (body instanceof Flux) {
            Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
            return super.writeWith(fluxBody.map(dataBuffer -> {
                // probably should reuse buffers
                byte[] content = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(content);
                //释放掉内存
                DataBufferUtils.release(dataBuffer);
                String responseBody = getResponseBody(exchange, this, content);
                responseBodys.append(responseBody);
                if (responseBody != null) {
                    // 修改响应体内容之前，需要设置响应体长度
                    getDelegate().getHeaders().setContentLength(responseBody.getBytes().length);
                    return bufferFactory.wrap(responseBody.getBytes(Charsets.UTF_8));
                }
                return bufferFactory.wrap(content);
            }));
        }
        return super.writeWith(body);
    }

    private final static boolean enableII18n = false;

    private final static String MESSAGE_KEY = "message";

    private String getResponseBody(ServerWebExchange exchange, ServerHttpResponse response, byte[] content) {
        if (response.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE)) {
            if (StrUtil.contains(Objects.requireNonNull(response.getHeaders().get(HttpHeaders.CONTENT_TYPE)).toString(), MediaType.APPLICATION_JSON_VALUE)) {
                String responseBody = StrUtil.str(content, Charsets.UTF_8);
                if(StrUtil.isBlank(responseBody)) {
                    return null;
                }
                //国际化处理
                if(enableII18n) {
                    JSONObject jsonObject = JSONObject.parseObject(responseBody);
                    String message = jsonObject.getString(MESSAGE_KEY);
                    if (StrUtil.isNotBlank(message)) {
                        //将提示语message作为key记录下来，更具语言场景提供，对应的国际化语言包
                        try {
                            message = messageSource.resolveCodeWithoutArguments(exchange, message);
                            jsonObject.put(MESSAGE_KEY, message);
                        } catch (NoSuchMessageException ignored) {
                            messageSource.insertMessage(message, message, exchange.getLocaleContext().getLocale());
                        }
                        return jsonObject.toJSONString();
                    } else {
                        return StrUtil.str(content, Charsets.UTF_8);
                    }
                }else{
                    return StrUtil.str(content, Charsets.UTF_8);
                }
            }
        }
        return null;
    }
}
