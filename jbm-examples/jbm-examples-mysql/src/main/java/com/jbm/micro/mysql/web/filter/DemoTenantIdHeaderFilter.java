package com.jbm.micro.mysql.web.filter;

import com.jbm.micro.mysql.tenant.DemoTenantLineHandler;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 演示用：从请求头 {@code X-Demo-Tenant-Id} 写入 {@link DemoTenantLineHandler}，请求结束后清理，便于 HTTP 层走多租户拦截逻辑。
 */
public class DemoTenantIdHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Demo-Tenant-Id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String raw = request.getHeader(HEADER_NAME);
        boolean applied = false;
        if (StringUtils.hasText(raw)) {
            try {
                DemoTenantLineHandler.setTenantId(Long.parseLong(raw.trim()));
                applied = true;
            } catch (NumberFormatException ignored) {
                // 非法头：不设置租户，与「无租户上下文」行为一致
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (applied) {
                DemoTenantLineHandler.clear();
            }
        }
    }
}
