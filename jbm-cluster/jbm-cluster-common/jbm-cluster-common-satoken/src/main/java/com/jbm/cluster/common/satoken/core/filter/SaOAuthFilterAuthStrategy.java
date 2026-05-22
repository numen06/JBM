package com.jbm.cluster.common.satoken.core.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.filter.SaFilterAuthStrategy;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.common.basic.context.SecurityContextHolder;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * 下游服务 Token 校验：用户 Sa-Token 登录态，或 Gateway/Feign 携带的 Id-Token 内部互信。
 */
public class SaOAuthFilterAuthStrategy implements SaFilterAuthStrategy {

    private static final Logger log = LoggerFactory.getLogger(SaOAuthFilterAuthStrategy.class);

    @Override
    public void run(Object r) {
        HttpServletRequest httpServletRequest = getCurrentRequest();
        if (httpServletRequest == null) {
            return;
        }

        log.debug("[认证] requestURI={}, Authorization={}, idToken={}, internal={}",
                httpServletRequest.getRequestURI(),
                mask(httpServletRequest.getHeader(JbmSecurityConstants.AUTHORIZATION_HEADER)),
                mask(httpServletRequest.getHeader(SaIdUtil.ID_TOKEN)),
                httpServletRequest.getHeader(JbmSecurityConstants.INTERNAL_SERVICE));

        try {
            StpUtil.checkLogin();
            log.debug("[认证] 通过: 用户 Token 有效");
            return;
        } catch (NotLoginException ignored) {
            log.debug("[认证] 用户 Token 未登录，尝试 Id-Token 内部互信");
        }

        try {
            SaIdUtil.checkCurrentRequestToken();
            recordInternalCaller(httpServletRequest);
            log.debug("[认证] 通过: Id-Token 内部互信");
        } catch (Exception e) {
            log.debug("[认证] 失败: {}", e.getMessage());
            throw NotLoginException.newInstance(StpUtil.getLoginType(), NotLoginException.INVALID_TOKEN);
        }
    }

    private static String mask(String value) {
        if (StrUtil.isBlank(value)) {
            return "null";
        }
        return value.substring(0, Math.min(30, value.length())) + "...";
    }

    private static HttpServletRequest getCurrentRequest() {
        try {
            return (HttpServletRequest)
                    org.springframework.web.context.request.RequestContextHolder
                            .currentRequestAttributes()
                            .resolveReference(org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST);
        } catch (Exception e) {
            return null;
        }
    }

    private static void recordInternalCaller(HttpServletRequest request) {
        String fromService = request.getHeader(JbmSecurityConstants.INTERNAL_SERVICE);
        if (StrUtil.isNotBlank(fromService)) {
            SecurityContextHolder.set(JbmSecurityConstants.FROM_SERVICE, fromService);
            SecurityContextHolder.set(JbmSecurityConstants.FROM_INSTANCE,
                    request.getHeader(JbmSecurityConstants.INTERNAL_INSTANCE));
            log.debug("[互信] 内部调用 from={}:{}", fromService,
                    request.getHeader(JbmSecurityConstants.INTERNAL_INSTANCE));
        }
    }
}
