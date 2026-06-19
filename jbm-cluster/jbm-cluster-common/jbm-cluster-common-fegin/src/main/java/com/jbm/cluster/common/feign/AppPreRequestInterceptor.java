package com.jbm.cluster.common.feign;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.cluster.core.security.InternalServiceTokenProvider;
import feign.RequestTemplate;
import jbm.framework.web.ServletUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 无用户 Token 时为内部 Feign 调用注入 OAuth2 Client Credentials JWT 及调用方身份 Header。
 */
public class AppPreRequestInterceptor implements PreRequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate, HttpServletRequest httpServletRequest) {
        if (shouldUseInternalTrust(requestTemplate)) {
            requestTemplate.removeHeader(JbmSecurityConstants.AUTHORIZATION_HEADER);
            applyInternalServiceToken(requestTemplate);
            return;
        }
        if (ObjectUtil.isNotEmpty(httpServletRequest)) {
            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
            String authentication = headers.get(JbmSecurityConstants.AUTHORIZATION_HEADER);
            if (StrUtil.isNotEmpty(authentication)) {
                return;
            }
        }
        applyInternalServiceToken(requestTemplate);
    }

    /** Gateway 验签/授权查库：必须使用内部 OAuth2 Client Credentials JWT，不能透传第三方 Bearer。 */
    private static boolean shouldUseInternalTrust(RequestTemplate requestTemplate) {
        String url = requestTemplate.url();
        if (StrUtil.isBlank(url)) {
            return false;
        }
        return url.contains("/apikey") || url.contains("/api?");
    }

    private void applyInternalServiceToken(RequestTemplate requestTemplate) {
        String authorization = InternalServiceTokenProvider.authorizationHeader();
        if (StrUtil.isNotBlank(authorization)) {
            requestTemplate.header(JbmSecurityConstants.INTERNAL_AUTHORIZATION_HEADER, authorization);
        }
        appendInternalIdentity(requestTemplate);
    }

    static void appendInternalIdentity(RequestTemplate requestTemplate) {
        requestTemplate.header(JbmSecurityConstants.INTERNAL_SERVICE, cn.hutool.extra.spring.SpringUtil.getApplicationName());
        requestTemplate.header(JbmSecurityConstants.INTERNAL_INSTANCE,
                cn.hutool.extra.spring.SpringUtil.getApplicationName() + ":"
                        + cn.hutool.extra.spring.SpringUtil.getProperty("server.port", "0"));
    }
}
