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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 *
 * 请求追加Token
 * @Created wesley.zhang
 * @Date 2022/5/19 19:13
 * @Description TODO
 */
public class AppPreRequestInterceptor implements PreRequestInterceptor {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AppPreRequestInterceptor.class);


    @Autowired
    private SaOAuth2Template saOAuth2Template;

    @Override
    public void apply(RequestTemplate requestTemplate, HttpServletRequest httpServletRequest) {
        if (ObjectUtil.isNotEmpty(httpServletRequest)) {
            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
            String authentication = headers.get(JbmSecurityConstants.AUTHORIZATION_HEADER);
            if (StrUtil.isEmpty(authentication)) {
                ClientTokenModel clientTokenModel = saOAuth2Template.generateClientToken(SpringUtil.getApplicationName(), "*");
                requestTemplate.header(SaIdUtil.ID_TOKEN, getTokenSafely());
                requestTemplate.header(JbmSecurityConstants.AUTHORIZATION_HEADER, StrUtil.emptyToDefault(SaManager.getConfig().getTokenPrefix(), "Bearer") + " " + clientTokenModel.clientToken);
            }
        } else {
            ClientTokenModel clientTokenModel = saOAuth2Template.generateClientToken(SpringUtil.getApplicationName(), "*");
            requestTemplate.header(SaIdUtil.ID_TOKEN, getTokenSafely());
            requestTemplate.header(JbmSecurityConstants.AUTHORIZATION_HEADER, StrUtil.emptyToDefault(SaManager.getConfig().getTokenPrefix(), "Bearer") + " " + clientTokenModel.clientToken);
        }
    }

    private String getTokenSafely() {
        try {
            return SaIdUtil.getToken();
        } catch (Exception e) {
            log.warn("[AppPreRequestInterceptor]获取IdToken失败(可能Redis不可用): {}", e.getMessage());
            return null;
        }
    }


}
