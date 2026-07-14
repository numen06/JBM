package com.jbm.cluster.common.feign;

import cn.dev33.satoken.id.SaIdUtil;
import cn.hutool.core.util.ObjectUtil;
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * feign 请求拦截器
 *
 * @author wesley.zhang
 */
@Slf4j
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final Set<String> INBOUND_HEADER_EXCLUDES = new HashSet<>(Arrays.asList(
            "content-length",
            "connection",
            "cookie",
            "host",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "proxy-connection",
            "set-cookie",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            JbmSecurityConstants.AUTHORIZATION_HEADER.toLowerCase(),
            SaIdUtil.ID_TOKEN.toLowerCase(),
            JbmSecurityConstants.FROM_SOURCE.toLowerCase(),
            FeignTokenContext.ACCESS_MODE_HEADER.toLowerCase(),
            IpUtils.X_FORWARDED_FOR.toLowerCase()
    ));

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        // 获取相关对象
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest httpServletRequest = requestAttributes == null ? null : ((ServletRequestAttributes) requestAttributes).getRequest();
        removeSecurityHeaders(requestTemplate);
        if (ObjectUtil.isNotEmpty(httpServletRequest)) {
            copyInboundHeaders(requestTemplate, httpServletRequest);
            // 配置客户端IP
            requestTemplate.removeHeader(IpUtils.X_FORWARDED_FOR);
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

    private static void copyInboundHeaders(RequestTemplate requestTemplate, HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return;
        }
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (INBOUND_HEADER_EXCLUDES.contains(headerName.toLowerCase())) {
                continue;
            }
            String headerValue = request.getHeader(headerName);
            if (headerValue != null) {
                requestTemplate.header(headerName, headerValue);
            }
        }
    }

    private static void removeSecurityHeaders(RequestTemplate requestTemplate) {
        requestTemplate.removeHeader(JbmSecurityConstants.AUTHORIZATION_HEADER);
        requestTemplate.removeHeader(SaIdUtil.ID_TOKEN);
        requestTemplate.removeHeader(JbmSecurityConstants.FROM_SOURCE);
    }

}
