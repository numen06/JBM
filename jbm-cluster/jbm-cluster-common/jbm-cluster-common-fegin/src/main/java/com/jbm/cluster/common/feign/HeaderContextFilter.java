package com.jbm.cluster.common.feign;

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
