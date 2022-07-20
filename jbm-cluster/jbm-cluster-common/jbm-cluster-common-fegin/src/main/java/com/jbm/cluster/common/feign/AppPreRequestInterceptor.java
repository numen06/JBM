package com.jbm.cluster.common.feign;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.dev33.satoken.oauth2.model.SaClientModel;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.mvc.ServletUtils;
import feign.RequestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @Created wesley.zhang
 * @Date 2022/5/19 19:13
 * @Description TODO
 */
public class AppPreRequestInterceptor implements PreRequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate, HttpServletRequest httpServletRequest) {
        if (ObjectUtil.isNotEmpty(httpServletRequest)) {
            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
            String authentication = headers.get(JbmSecurityConstants.AUTHORIZATION_HEADER);
            if (StrUtil.isEmpty(authentication)) {
//                ClientTokenModel clientTokenModel = SaOAuth2Util.generateClientToken("1001", "*");
//                String authorization = "Bearer" + " " + clientTokenModel.clientToken;
//                requestTemplate.header(JbmSecurityConstants.AUTHORIZATION_HEADER, authorization);
                //自动生成一个客户端的token
                SaIdUtil.getToken();
                ClientTokenModel clientTokenModel = SaOAuth2Util.generateClientToken(SpringUtil.getApplicationName(), "*");
                requestTemplate.header(JbmSecurityConstants.AUTHORIZATION_HEADER, SaManager.getConfig().getTokenPrefix() + " " + clientTokenModel.clientToken);
            }
        } else {
            SaOAuth2Template saOAuth2Template = new SaOAuth2Template() {
                @Override
                public SaClientModel getClientModel(String clientToken) {
                    return new SaClientModel()
                            .setClientId(SpringUtil.getApplicationName())
                            .setClientSecret(SaIdUtil.getToken())
                            .setAllowUrl("*")
                            .setContractScope("*")
                            .setIsAutoMode(true);
                }
            };
            ClientTokenModel clientTokenModel = saOAuth2Template.generateClientToken(SpringUtil.getApplicationName(), "*");
            requestTemplate.header(JbmSecurityConstants.AUTHORIZATION_HEADER, SaManager.getConfig().getTokenPrefix() + " " + clientTokenModel.clientToken);
        }
    }


}
