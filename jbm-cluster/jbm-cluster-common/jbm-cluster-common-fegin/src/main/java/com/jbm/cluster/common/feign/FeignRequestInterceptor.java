package com.jbm.cluster.common.feign;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.common.basic.utils.IpUtils;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jbm.framework.web.ServletUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
        // 获取相关对象
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest httpServletRequest = requestAttributes == null ? null : ((ServletRequestAttributes) requestAttributes).getRequest();
        if (ObjectUtil.isNotEmpty(httpServletRequest)) {
            // 传递用户信息请求头，防止丢失
            String userId = ServletUtils.getHeader(httpServletRequest, JbmSecurityConstants.DETAILS_USER_ID);
            if (StrUtil.isNotEmpty(userId)) {
                requestTemplate.header(JbmSecurityConstants.DETAILS_USER_ID, userId);
            }
            String userName = ServletUtils.getHeader(httpServletRequest, JbmSecurityConstants.DETAILS_USERNAME);
            if (StrUtil.isNotEmpty(userName)) {
                requestTemplate.header(JbmSecurityConstants.DETAILS_USERNAME, userName);
            }
            String authentication = ServletUtils.getHeader(httpServletRequest, JbmSecurityConstants.AUTHORIZATION_HEADER);
            if (StrUtil.isNotEmpty(authentication)) {
                requestTemplate.header(JbmSecurityConstants.AUTHORIZATION_HEADER, authentication);
            }
            // 配置客户端IP
            requestTemplate.header(IpUtils.X_FORWARDED_FOR, IpUtils.getRequestIp(ServletUtils.getRequest()));
        }
        //以上标准内容注入完成之后，搜索自定义配置
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


    }

}