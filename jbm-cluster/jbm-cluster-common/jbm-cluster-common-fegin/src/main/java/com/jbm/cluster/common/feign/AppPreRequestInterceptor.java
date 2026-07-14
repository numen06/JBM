package com.jbm.cluster.common.feign;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import feign.RequestTemplate;
import jbm.framework.web.ServletUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;

/**
 * Feign 出站 Token 注入：默认使用 ClientToken 跨权限访问，显式要求用户委托时才透传当前用户 Token。
 */
public class AppPreRequestInterceptor implements PreRequestInterceptor {

    @Autowired
    private SaOAuth2Template saOAuth2Template;

    @Override
    public void apply(RequestTemplate requestTemplate, HttpServletRequest httpServletRequest) {
        if (shouldRelayUserToken(requestTemplate)) {
            String authentication = resolveAuthorization(httpServletRequest, requestTemplate);
            removeInnerHeaders(requestTemplate);
            requestTemplate.removeHeader(JbmSecurityConstants.AUTHORIZATION_HEADER);
            if (StrUtil.isNotEmpty(authentication)) {
                requestTemplate.header(JbmSecurityConstants.AUTHORIZATION_HEADER, authentication);
            }
            return;
        }

        appendInnerHeaders(requestTemplate);
        ClientTokenModel clientTokenModel = saOAuth2Template.generateClientToken(SpringUtil.getApplicationName(), "*");
        requestTemplate.removeHeader(JbmSecurityConstants.AUTHORIZATION_HEADER);
        requestTemplate.header(JbmSecurityConstants.AUTHORIZATION_HEADER,
                StrUtil.emptyToDefault(SaManager.getConfig().getTokenPrefix(), "Bearer") + " " + clientTokenModel.clientToken);
    }

    private static void appendInnerHeaders(RequestTemplate requestTemplate) {
        removeInnerHeaders(requestTemplate);
        requestTemplate.header(SaIdUtil.ID_TOKEN, SaIdUtil.getToken());
        requestTemplate.header(JbmSecurityConstants.FROM_SOURCE, JbmSecurityConstants.INNER);
    }

    private static void removeInnerHeaders(RequestTemplate requestTemplate) {
        requestTemplate.removeHeader(SaIdUtil.ID_TOKEN);
        requestTemplate.removeHeader(JbmSecurityConstants.FROM_SOURCE);
    }

    private static boolean shouldRelayUserToken(RequestTemplate requestTemplate) {
        Collection<String> accessModeHeaders = requestTemplate.headers().get(FeignTokenContext.ACCESS_MODE_HEADER);
        requestTemplate.removeHeader(FeignTokenContext.ACCESS_MODE_HEADER);
        if (ObjectUtil.isNotEmpty(accessModeHeaders) && accessModeHeaders.stream().anyMatch(FeignTokenContext.ACCESS_MODE_USER::equalsIgnoreCase)) {
            return true;
        }
        return FeignTokenContext.isUserTokenRelay();
    }

    private static String resolveAuthorization(HttpServletRequest httpServletRequest, RequestTemplate requestTemplate) {
        if (ObjectUtil.isNotEmpty(httpServletRequest)) {
            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
            String authorization = headers.get(JbmSecurityConstants.AUTHORIZATION_HEADER);
            if (StrUtil.isNotEmpty(authorization)) {
                return authorization;
            }
        }
        Collection<String> authorizationHeaders = requestTemplate.headers().get(JbmSecurityConstants.AUTHORIZATION_HEADER);
        if (ObjectUtil.isEmpty(authorizationHeaders)) {
            return null;
        }
        return authorizationHeaders.stream().findFirst().orElse(null);
    }

}
