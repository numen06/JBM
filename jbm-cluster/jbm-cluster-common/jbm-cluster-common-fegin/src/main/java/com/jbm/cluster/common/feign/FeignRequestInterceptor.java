package com.jbm.cluster.common.feign;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.common.basic.utils.IpUtils;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.mvc.ServletUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * feign 请求拦截器
 *
 * @author wesley.zhang
 */
@Slf4j
public class FeignRequestInterceptor implements RequestInterceptor {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        HttpServletRequest httpServletRequest = ServletUtils.getRequest();
        if (ObjectUtil.isNull(httpServletRequest))
            return;
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
        Map<String, PreRequestInterceptor> preRequestInterceptorMap = applicationContext.getBeansOfType(PreRequestInterceptor.class);
        preRequestInterceptorMap.forEach(new BiConsumer<String, PreRequestInterceptor>() {
            @Override
            public void accept(String s, PreRequestInterceptor preRequestInterceptor) {
                try {
                    preRequestInterceptor.apply(requestTemplate, httpServletRequest);
                } catch (Exception e) {
                    log.error("Fegin预处理器失败", e);
                }
            }
        });

        // 配置客户端IP
        requestTemplate.header("X-Forwarded-For", IpUtils.getRequestIp(ServletUtils.getRequest()));
    }
}