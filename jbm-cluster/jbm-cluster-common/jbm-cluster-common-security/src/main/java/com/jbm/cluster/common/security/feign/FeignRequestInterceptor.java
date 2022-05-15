package com.jbm.cluster.common.security.feign;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.common.basic.utils.IpUtils;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.mvc.ServletUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * feign 请求拦截器
 *
 * @author wesley.zhang
 */
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        HttpServletRequest httpServletRequest = ServletUtils.getRequest();
        if (ObjectUtil.isNotNull(httpServletRequest)) {
            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
            // 传递用户信息请求头，防止丢失
            String userId = headers.get(JbmSecurityConstants.DETAILS_USER_ID);
            if (StrUtil.isNotEmpty(userId)) {
                requestTemplate.header(JbmSecurityConstants.DETAILS_USER_ID, userId);
            }
            String userName = headers.get(JbmSecurityConstants.DETAILS_USERNAME);
            if (StrUtil.isNotEmpty(userName)) {
                requestTemplate.header(JbmSecurityConstants.DETAILS_USERNAME, userName);
            }
            String authentication = headers.get(JbmSecurityConstants.AUTHORIZATION_HEADER);
            if (StrUtil.isNotEmpty(authentication)) {
                requestTemplate.header(JbmSecurityConstants.AUTHORIZATION_HEADER, authentication);
            }
            // 配置客户端IP
            requestTemplate.header("X-Forwarded-For", IpUtils.getRequestIp(ServletUtils.getRequest()));
        }
    }
}