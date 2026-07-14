package com.jbm.cluster.common.security.interceptor;

import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.interceptor.SaAnnotationInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 只有经过内部 Id-Token 互信的 Feign 调用才跳过用户权限注解。
 */
public class JbmSaAnnotationInterceptor extends SaAnnotationInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!isUserLoggedIn() && isTrustedInternalCall(request)) {
            validateInternalToken();
            return true;
        }
        return super.preHandle(request, response, handler);
    }

    boolean isUserLoggedIn() {
        return StpUtil.isLogin();
    }

    void validateInternalToken() {
        SaIdUtil.checkCurrentRequestToken();
    }

    private static boolean isTrustedInternalCall(HttpServletRequest request) {
        return JbmSecurityConstants.INNER.equals(request.getHeader(JbmSecurityConstants.FROM_SOURCE))
                && StrUtil.isNotBlank(request.getHeader(SaIdUtil.ID_TOKEN));
    }
}
