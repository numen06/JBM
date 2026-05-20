package com.jbm.examples.extendfield.designer.tenant;

import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import jbm.framework.boot.autoconfigure.extendfield.tenant.ExtendFieldTenantResolver;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DemoTenantIdHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Demo-Tenant-Id";

    private final ExtendFieldProperties extendFieldProperties;

    public DemoTenantIdHeaderFilter(ExtendFieldProperties extendFieldProperties) {
        this.extendFieldProperties = extendFieldProperties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Long tenantId = resolveBusinessTenantId(request);
        boolean applied = tenantId != null;
        if (applied) {
            DemoTenantLineHandler.setTenantId(tenantId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (applied) {
                DemoTenantLineHandler.clear();
            }
        }
    }

    private Long resolveBusinessTenantId(HttpServletRequest request) {
        String raw = request.getHeader(HEADER_NAME);
        if (StringUtils.hasText(raw)) {
            try {
                return Long.parseLong(raw.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return ExtendFieldTenantResolver.resolveTenantIdAsLong(extendFieldProperties);
    }
}
