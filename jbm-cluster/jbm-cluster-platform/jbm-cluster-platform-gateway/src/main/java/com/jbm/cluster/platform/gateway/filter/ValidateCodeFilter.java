package com.jbm.cluster.platform.gateway.filter;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.net.URLDecoder;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.platform.gateway.config.properties.CaptchaProperties;
import com.jbm.cluster.platform.gateway.service.ValidateCodeService;
import com.jbm.cluster.platform.gateway.utils.WebFluxUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

/**
 * 验证码过滤器
 *
 * @author wesley.zhang
 */
@Slf4j
@Component
public class ValidateCodeFilter extends AbstractGatewayFilterFactory<Object> {
    private static final String[] OAUTH_VALIDATE_URL = new String[]{
            "/oauth2/doLogin", "/oauth2/token"};
    private static final String[] LEGACY_VALIDATE_URL = new String[]{
            "/auth/login", "/auth/register"};
    private static final String CODE = "code";
    private static final String UUID = "uuid";
    private static final String VCODE = "vcode";
    private static final String LOGIN_TYPE = "loginType";
    private static final String LOGIN_TYPE_ALIAS = "login_type";
    private static final String GRANT_TYPE = "grant_type";
    private static final String GRANT_PASSWORD = "password";
    @Autowired
    private ValidateCodeService validateCodeService;
    @Autowired
    private CaptchaProperties captchaProperties;

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            if (!captchaProperties.getEnabled()) {
                return chain.filter(exchange);
            }

            boolean oauthPath = StrUtil.containsAnyIgnoreCase(path, OAUTH_VALIDATE_URL);
            boolean legacyPath = StrUtil.containsAnyIgnoreCase(path, LEGACY_VALIDATE_URL);
            if (!oauthPath && !legacyPath) {
                return chain.filter(exchange);
            }

            return DataBufferUtils.join(request.getBody())
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        String body = new String(bytes, StandardCharsets.UTF_8);

                        try {
                            String contentType = request.getHeaders().getFirst("Content-Type");
                            Dict obj = parseRequestBody(body, contentType);
                            if (oauthPath) {
                                if (!shouldSkipOAuthVcode(obj, request, path)) {
                                    validateCodeService.verifyVcode(obj.getStr(VCODE));
                                }
                            } else {
                                validateCodeService.checkCaptcha(obj.getStr(CODE), obj.getStr(UUID));
                            }
                        } catch (Exception e) {
                            log.warn("[验证码]验证码校验失败, uri:{}", path);
                            return WebFluxUtils.webFluxResponseWriter(exchange.getResponse(), HttpStatus.BAD_REQUEST,
                                    "验证码错误,请重试", null, HttpStatus.BAD_REQUEST.value());
                        }

                        ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(request) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                                return Flux.just(buffer);
                            }
                        };
                        return chain.filter(exchange.mutate().request(decorator).build());
                    })
                    .switchIfEmpty(chain.filter(exchange));
        };
    }

    private Dict parseRequestBody(String body, String contentType) {
        if (StrUtil.isBlank(body)) {
            return Dict.create();
        }
        if (StrUtil.containsIgnoreCase(contentType, "application/json")) {
            return JSON.parseObject(body, Dict.class);
        }
        Dict dict = Dict.create();
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            dict.set(key, value);
        }
        return dict;
    }

    /**
     * 短信登录、授权码/刷新令牌换票等非密码登录场景跳过图形 vcode
     */
    private boolean shouldSkipOAuthVcode(Dict obj, ServerHttpRequest request, String path) {
        if (isSmsLogin(obj, request)) {
            return true;
        }
        if (StrUtil.containsAnyIgnoreCase(path, "/oauth2/token")) {
            String grantType = readParam(obj, request, GRANT_TYPE);
            if (StrUtil.isNotBlank(grantType) && !GRANT_PASSWORD.equalsIgnoreCase(grantType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSmsLogin(Dict obj, ServerHttpRequest request) {
        String loginType = readParam(obj, request, LOGIN_TYPE, LOGIN_TYPE_ALIAS);
        return "SMS".equalsIgnoreCase(loginType);
    }

    private String readParam(Dict obj, ServerHttpRequest request, String... keys) {
        for (String key : keys) {
            if (obj != null) {
                String value = obj.getStr(key);
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            }
            if (request != null) {
                String queryValue = request.getQueryParams().getFirst(key);
                if (StrUtil.isNotBlank(queryValue)) {
                    return queryValue;
                }
            }
        }
        return null;
    }
}
