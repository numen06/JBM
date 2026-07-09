package com.jbm.cluster.common.security.interceptor;

import cn.dev33.satoken.interceptor.SaAnnotationInterceptor;
import com.jbm.cluster.common.security.context.InnerAuthContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 内部服务链已验证 ClientToken 时，跳过 @SaCheckPermission 等用户权限注解。
 */
public class JbmSaAnnotationInterceptor extends SaAnnotationInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (InnerAuthContext.shouldSkipPermissionCheck()) {
            return true;
        }
        return super.preHandle(request, response, handler);
    }
}
