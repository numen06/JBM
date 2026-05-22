package com.jbm.cluster.auth.service;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.event.auth.LoginFailureEvent;
import com.jbm.cluster.auth.model.LoginProcessModel;
import com.jbm.cluster.common.basic.utils.IpUtils;
import jbm.framework.web.ServletUtils;
import jbm.framework.web.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class LoginLifecyclePublisher {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public void publishLoginFailure(LoginProcessModel model, String reason) {
        HttpServletRequest request = WebUtils.getHttpServletRequest();
        String ip = request != null ? IpUtils.getRequestIp(request) : null;
        applicationEventPublisher.publishEvent(new LoginFailureEvent(this,
                model != null ? model.getUsername() : null,
                model != null && model.getLoginType() != null ? model.getLoginType().name() : null,
                model != null ? model.getClientId() : null,
                ip,
                reason));
    }

    public void publishLoginFailure(String username, String loginType, String clientId, String reason) {
        HttpServletRequest request = WebUtils.getHttpServletRequest();
        String ip = request != null ? IpUtils.getRequestIp(request) : null;
        applicationEventPublisher.publishEvent(new LoginFailureEvent(this, username, loginType, clientId, ip, reason));
    }

    public static String currentUserAgent() {
        try {
            if (ServletUtils.getRequest() != null) {
                return ServletUtils.getRequest().getHeader(HttpHeaders.USER_AGENT);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String currentIp() {
        try {
            HttpServletRequest request = WebUtils.getHttpServletRequest();
            if (request != null) {
                return IpUtils.getRequestIp(request);
            }
        } catch (Exception ignored) {
        }
        return StrUtil.EMPTY;
    }
}
