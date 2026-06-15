from pathlib import Path

def w(path, content):
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_text(content, encoding="utf-8", newline="\n")
    print("ok", path)

ROOT = Path(__file__).resolve().parents[3] / "jbm-cluster/jbm-cluster-api/jbm-cluster-api-basic/src/main/java"

w(ROOT / "jbm-framework/jbm-framework-autoconfigure/jbm-framework-autoconfigure-fegin/src/main/java/jbm/framework/boot/autoconfigure/feign/RemoteServiceException.java", """package jbm.framework.boot.autoconfigure.feign;

import lombok.Getter;

@Getter
public class RemoteServiceException extends RuntimeException {
    private final Integer code;
    private final String remoteUrl;

    public RemoteServiceException(Integer code, String message, String remoteUrl) {
        super(message);
        this.code = code;
        this.remoteUrl = remoteUrl;
    }

    public RemoteServiceException(int httpStatus, String message, String remoteUrl) {
        this(Integer.valueOf(httpStatus), message, remoteUrl);
    }

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
        this.code = null;
        this.remoteUrl = null;
    }
}
""")

w(ROOT / "jbm-framework/jbm-framework-autoconfigure/jbm-framework-autoconfigure-fegin/src/main/java/jbm/framework/boot/autoconfigure/feign/UnwrapResultBodyDecoder.java", """package jbm.framework.boot.autoconfigure.feign;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import com.jbm.framework.metadata.bean.ResultBody;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class UnwrapResultBodyDecoder implements Decoder {
    private final Decoder delegate;

    public UnwrapResultBodyDecoder(Decoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        if (type instanceof Class && ResultBody.class.isAssignableFrom((Class<?>) type)) {
            return delegate.decode(response, type);
        }
        if (response.body() == null) {
            return null;
        }
        byte[] body = Util.toByteArray(response.body().asInputStream());
        if (body.length == 0) {
            return null;
        }
        String json = new String(body, StandardCharsets.UTF_8);
        JSONObject root = JSON.parseObject(json);
        if (root == null || !root.containsKey("success")) {
            return delegate.decode(response.toBuilder().body(body).build(), type);
        }
        if (Boolean.FALSE.equals(root.getBoolean("success"))) {
            throw new RemoteServiceException(root.getInteger("code"), root.getString("message"), response.request().url());
        }
        Object result = root.get("result");
        if (result == null) {
            return null;
        }
        if (type instanceof Class) {
            return JSON.parseObject(JSON.toJSONString(result), (Class<?>) type);
        }
        return JSON.parseObject(JSON.toJSONString(result), type);
    }
}
""")

w(ROOT / "jbm-cluster/jbm-cluster-common/jbm-cluster-common-fegin/src/main/java/com/jbm/cluster/common/feign/FeignClientErrorDecoder.java", """package com.jbm.cluster.common.feign;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import feign.Response;
import feign.codec.ErrorDecoder;
import jbm.framework.boot.autoconfigure.feign.RemoteServiceException;

import java.nio.charset.StandardCharsets;

public class FeignClientErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            if (response.body() != null) {
                String json = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JSONObject root = JSON.parseObject(json);
                if (root != null && root.containsKey("success")) {
                    return new RemoteServiceException(root.getInteger("code"), root.getString("message"), response.request().url());
                }
            }
        } catch (Exception ignored) {
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
""")

w(ROOT / "jbm-cluster/jbm-cluster-common/jbm-cluster-common-fegin/src/main/java/com/jbm/cluster/common/feign/FeignExceptionHandler.java", """package com.jbm.cluster.common.feign;

import jbm.framework.boot.autoconfigure.feign.RemoteServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(100)
public class FeignExceptionHandler {

    @ExceptionHandler(RemoteServiceException.class)
    public ResultBody<Void> handleRemoteServiceException(RemoteServiceException ex) {
        Integer code = ex.getCode();
        if (code != null) {
            return ResultBody.error(code, ex.getMessage());
        }
        return ResultBody.error(ex.getMessage());
    }
}
""")

w(ROOT / "jbm-cluster/jbm-cluster-common/jbm-cluster-common-fegin/src/main/java/com/jbm/cluster/common/feign/HeaderContextFilter.java", """package com.jbm.cluster.common.feign;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class HeaderContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Map<String, String> extra = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            if (StrUtil.startWithIgnoreCase(name, JbmSecurityConstants.CONTEXT_HEADER_PREFIX)) {
                extra.put(name, request.getHeader(name));
            }
        }
        if (extra.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
            private final Map<String, String> headers = extra;

            @Override
            public String getHeader(String name) {
                return headers.getOrDefault(name, super.getHeader(name));
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                Vector<String> v = new Vector<>();
                Enumeration<String> base = super.getHeaderNames();
                while (base.hasMoreElements()) {
                    v.add(base.nextElement());
                }
                v.addAll(headers.keySet());
                return v.elements();
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (headers.containsKey(name)) {
                    return Collections.enumeration(Collections.singletonList(headers.get(name)));
                }
                return super.getHeaders(name);
            }
        };
        filterChain.doFilter(wrapper, response);
    }
}
""")

