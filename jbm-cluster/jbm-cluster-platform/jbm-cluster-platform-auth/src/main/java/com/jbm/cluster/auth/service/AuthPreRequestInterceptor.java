package com.jbm.cluster.auth.service;

import cn.dev33.satoken.id.SaIdTemplate;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.common.feign.PreRequestInterceptor;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.mvc.ServletUtils;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @Created wesley.zhang
 * @Date 2022/5/19 19:13
 * @Description TODO
 */
@Component
public class AuthPreRequestInterceptor implements PreRequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate, HttpServletRequest httpServletRequest) {
        if (ObjectUtil.isNotEmpty(httpServletRequest)) {
            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
            String authentication = headers.get(JbmSecurityConstants.AUTHORIZATION_HEADER);
            if (StrUtil.isEmpty(authentication)) {
//                ClientTokenModel clientTokenModel = SaOAuth2Util.generateClientToken("1001", "*");
//                String authorization = "Bearer" + " " + clientTokenModel.clientToken;
//                requestTemplate.header(JbmSecurityConstants.AUTHORIZATION_HEADER, authorization);
                requestTemplate.header(SaIdTemplate.ID_TOKEN, SaIdUtil.getToken());
            }
        } else {
            requestTemplate.header(SaIdUtil.ID_TOKEN, SaIdUtil.getToken());
        }
    }
}
