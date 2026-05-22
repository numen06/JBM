package com.jbm.cluster.common.feign;

import cn.dev33.satoken.id.SaIdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import feign.RequestTemplate;
import jbm.framework.web.ServletUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 无用户 Token 时为内部 Feign 调用注入 Sa-Token Id-Token 及调用方身份 Header。
 */
public class AppPreRequestInterceptor implements PreRequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate, HttpServletRequest httpServletRequest) {
        if (ObjectUtil.isNotEmpty(httpServletRequest)) {
            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
            String authentication = headers.get(JbmSecurityConstants.AUTHORIZATION_HEADER);
            if (StrUtil.isNotEmpty(authentication)) {
                return;
            }
        }
        applyInternalIdToken(requestTemplate);
    }

    private void applyInternalIdToken(RequestTemplate requestTemplate) {
        requestTemplate.header(SaIdUtil.ID_TOKEN, SaIdUtil.getToken());
        appendInternalIdentity(requestTemplate);
    }

    static void appendInternalIdentity(RequestTemplate requestTemplate) {
        requestTemplate.header(JbmSecurityConstants.INTERNAL_SERVICE, cn.hutool.extra.spring.SpringUtil.getApplicationName());
        requestTemplate.header(JbmSecurityConstants.INTERNAL_INSTANCE,
                cn.hutool.extra.spring.SpringUtil.getApplicationName() + ":"
                        + cn.hutool.extra.spring.SpringUtil.getProperty("server.port", "0"));
    }
}
