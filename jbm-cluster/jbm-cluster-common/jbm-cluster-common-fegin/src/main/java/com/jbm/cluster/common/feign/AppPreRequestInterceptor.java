package com.jbm.cluster.common.feign;

import cn.dev33.satoken.SaManager;
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
 * 无用户 Token 时为内部 Feign 调用注入 OAuth2 ClientToken（Redis）及调用方身份 Header。
 */
public class AppPreRequestInterceptor implements PreRequestInterceptor {

    @Autowired
    private SaOAuth2Template saOAuth2Template;

    @Override
    public void apply(RequestTemplate requestTemplate, HttpServletRequest httpServletRequest) {
        if (ObjectUtil.isNotEmpty(httpServletRequest)) {
            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
            String authentication = headers.get(JbmSecurityConstants.AUTHORIZATION_HEADER);
            if (StrUtil.isNotEmpty(authentication)) {
                return;
            }
        }
        applyInternalClientToken(requestTemplate);
    }

    private void applyInternalClientToken(RequestTemplate requestTemplate) {
        ClientTokenModel clientTokenModel = saOAuth2Template.generateClientToken(SpringUtil.getApplicationName(), "*");
        String prefix = StrUtil.emptyToDefault(SaManager.getConfig().getTokenPrefix(), "Bearer");
        requestTemplate.header(JbmSecurityConstants.AUTHORIZATION_HEADER, prefix + " " + clientTokenModel.clientToken);
        appendInternalIdentity(requestTemplate);
    }

    static void appendInternalIdentity(RequestTemplate requestTemplate) {
        requestTemplate.header(JbmSecurityConstants.INTERNAL_SERVICE, SpringUtil.getApplicationName());
        requestTemplate.header(JbmSecurityConstants.INTERNAL_INSTANCE,
                SpringUtil.getApplicationName() + ":" + SpringUtil.getProperty("server.port", "0"));
    }
}
