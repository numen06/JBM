package com.jbm.cluster.common.security.interceptor;

import cn.dev33.satoken.interceptor.SaAnnotationInterceptor;
import cn.dev33.satoken.stp.StpUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 无用户登录态（ClientToken 等服务间调用）时跳过 {@code @SaCheckLogin} / {@code @SaCheckPermission}。
 * Token 合法性已由 {@code SaOAuthFilterAuthStrategy} 统一校验。
 */
public class JbmSaAnnotationInterceptor extends SaAnnotationInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!StpUtil.isLogin()) {
            return true;
        }
        return super.preHandle(request, response, handler);
    }
}
