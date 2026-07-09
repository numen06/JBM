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
import java.util.Map;

/**
 * Feign 出站 Token 注入：用户委托链透传 AccessToken，内部服务链使用 ClientToken。
 */
public class AppPreRequestInterceptor implements PreRequestInterceptor {

    @Autowired
    private SaOAuth2Template saOAuth2Template;

    @Override
    public void apply(RequestTemplate requestTemplate, HttpServletRequest httpServletRequest) {
        appendInnerHeaders(requestTemplate);

        String authentication = resolveAuthorization(httpServletRequest, requestTemplate);
        if (StrUtil.isNotEmpty(authentication)) {
            return;
        }

        ClientTokenModel clientTokenModel = saOAuth2Template.generateClientToken(SpringUtil.getApplicationName(), "*");
        requestTemplate.header(JbmSecurityConstants.AUTHORIZATION_HEADER,
                StrUtil.emptyToDefault(SaManager.getConfig().getTokenPrefix(), "Bearer") + " " + clientTokenModel.clientToken);
    }

    private static void appendInnerHeaders(RequestTemplate requestTemplate) {
        requestTemplate.header(SaIdUtil.ID_TOKEN, SaIdUtil.getToken());
        requestTemplate.header(JbmSecurityConstants.FROM_SOURCE, JbmSecurityConstants.INNER);
    }

    private static String resolveAuthorization(HttpServletRequest httpServletRequest, RequestTemplate requestTemplate) {
        if (ObjectUtil.isNotEmpty(httpServletRequest)) {
            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
            String authorization = headers.get(JbmSecurityConstants.AUTHORIZATION_HEADER);
            if (StrUtil.isNotEmpty(authorization)) {
                return authorization;
            }
        }
        return requestTemplate.headers().get(JbmSecurityConstants.AUTHORIZATION_HEADER).stream().findFirst().orElse(null);
    }

}
