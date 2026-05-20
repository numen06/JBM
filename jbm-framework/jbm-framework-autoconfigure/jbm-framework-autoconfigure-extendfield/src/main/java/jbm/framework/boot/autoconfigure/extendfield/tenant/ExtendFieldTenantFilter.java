package jbm.framework.boot.autoconfigure.extendfield.tenant;

import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 从请求头解析租户；未传且开启默认模块时写入 {@link ExtendFieldTenantResolver#resolveTenantId}。
 */
@Component
@Order(50)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "jbm.extend-field.tenant", name = "enabled", havingValue = "true")
public class ExtendFieldTenantFilter extends OncePerRequestFilter {

    @Resource
    private ExtendFieldProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = properties.getTenant().getHeader();
        String raw = request.getHeader(header);
        boolean applied = false;
        if (StringUtils.hasText(raw)) {
            ExtendFieldTenantContext.setTenantId(raw.trim());
            applied = true;
        } else if (properties.getTenant().isUseDefaultWhenMissing()) {
            String fallback = ExtendFieldTenantResolver.resolveTenantId(properties);
            if (StringUtils.hasText(fallback)) {
                ExtendFieldTenantContext.setTenantId(fallback);
                applied = true;
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (applied) {
                ExtendFieldTenantContext.clear();
            }
        }
    }
}
