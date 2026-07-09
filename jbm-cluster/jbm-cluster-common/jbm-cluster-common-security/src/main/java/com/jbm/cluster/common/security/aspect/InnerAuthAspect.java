package com.jbm.cluster.common.security.aspect;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.common.basic.context.SecurityContextHolder;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.common.security.context.InnerAuthContext;
import com.jbm.cluster.core.annotation.InnerAuth;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import jbm.framework.web.ServletUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 内部 Feign 调用标记：Token 合法性由 {@code SaOAuthFilterAuthStrategy} 统一校验，
 * 此处仅在使用 ClientToken（无用户登录态）时跳过 {@code @SaCheckPermission} 等用户权限注解。
 */
@Slf4j
@Aspect
@Component
public class InnerAuthAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.stereotype.Controller)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        InnerAuth innerAuth = resolveInnerAuth(joinPoint);
        if (innerAuth == null) {
            return joinPoint.proceed();
        }

        try {
            InnerAuthContext.setValidated(true);
            if (!StpUtil.isLogin()) {
                InnerAuthContext.setSkipPermissionCheck(true);
                log.debug("内部接口 {} 使用服务 Token，跳过用户权限注解校验", joinPoint.getSignature().getName());
            }
            if (innerAuth.isUser()) {
                restoreUserContext();
            }
            return joinPoint.proceed();
        } finally {
            InnerAuthContext.clear();
        }
    }

    private static InnerAuth resolveInnerAuth(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        InnerAuth innerAuth = AnnotatedElementUtils.findAnnotation(method, InnerAuth.class);
        if (innerAuth != null) {
            return innerAuth;
        }
        for (Class<?> iface : method.getDeclaringClass().getInterfaces()) {
            try {
                Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
                innerAuth = AnnotatedElementUtils.findAnnotation(ifaceMethod, InnerAuth.class);
                if (innerAuth != null) {
                    return innerAuth;
                }
            } catch (NoSuchMethodException ignored) {
                // continue
            }
        }
        return null;
    }

    private static void restoreUserContext() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return;
        }
        String userId = ServletUtils.getHeader(request, JbmSecurityConstants.DETAILS_USER_ID);
        String username = ServletUtils.getHeader(request, JbmSecurityConstants.DETAILS_USERNAME);
        if (StrUtil.isNotEmpty(userId)) {
            SecurityContextHolder.setUserId(userId);
        }
        if (StrUtil.isNotEmpty(username)) {
            SecurityContextHolder.setUserName(username);
        }
        if (StpUtil.isLogin()) {
            LoginHelper.initCache();
        }
    }

    private static HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes == null ? null : attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }
}
