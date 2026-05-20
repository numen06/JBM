package com.jbm.cluster.center.config;

import com.jbm.cluster.common.satoken.utils.LoginHelper;
import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import jbm.framework.boot.autoconfigure.extendfield.tenant.ExtendFieldTenantContext;
import jbm.framework.boot.autoconfigure.extendfield.tenant.ExtendFieldTenantResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Center 租户桥接：请求参数/Header → 登录 appId → 默认模块，写入扩展字段上下文。
 */
@Component
@Order(100)
@ConditionalOnProperty(prefix = "jbm.extend-field.tenant", name = "enabled", havingValue = "true")
public class ClusterExtendFieldTenantBridge implements WebMvcConfigurer {

    @Resource
    private ExtendFieldProperties extendFieldProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String tenantId = resolveTenantId(request);
                if (StringUtils.hasText(tenantId)) {
                    ExtendFieldTenantContext.setTenantId(tenantId);
                    request.setAttribute(ClusterExtendFieldTenantBridge.class.getName() + ".applied", Boolean.TRUE);
                }
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                      Object handler, Exception ex) {
                if (Boolean.TRUE.equals(request.getAttribute(
                        ClusterExtendFieldTenantBridge.class.getName() + ".applied"))) {
                    ExtendFieldTenantContext.clear();
                }
            }
        }).addPathPatterns("/**").order(100);
    }

    private String resolveTenantId(HttpServletRequest request) {
        String header = extendFieldProperties.getTenant().getHeader();
        String fromHeader = request.getHeader(header);
        if (StringUtils.hasText(fromHeader)) {
            return fromHeader.trim();
        }
        String param = request.getParameter(header);
        if (StringUtils.hasText(param)) {
            return param.trim();
        }
        com.jbm.cluster.api.model.auth.JbmLoginUser loginUser = LoginHelper.softGetLoginUser();
        if (loginUser != null && loginUser.getAppId() != null) {
            return String.valueOf(loginUser.getAppId());
        }
        return ExtendFieldTenantResolver.resolveTenantId(extendFieldProperties);
    }
}
