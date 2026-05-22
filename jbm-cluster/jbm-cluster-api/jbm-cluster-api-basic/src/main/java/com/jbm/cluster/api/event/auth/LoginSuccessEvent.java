package com.jbm.cluster.api.event.auth;

import com.jbm.cluster.api.model.auth.JbmLoginUser;
import org.springframework.context.ApplicationEvent;

public class LoginSuccessEvent extends ApplicationEvent {

    private final JbmLoginUser loginUser;
    private final String loginType;
    private final String clientId;
    private final String device;
    private final String ip;
    private final String userAgent;

    public LoginSuccessEvent(Object source, JbmLoginUser loginUser, String loginType, String clientId,
                             String device, String ip, String userAgent) {
        super(source);
        this.loginUser = loginUser;
        this.loginType = loginType;
        this.clientId = clientId;
        this.device = device;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public JbmLoginUser getLoginUser() {
        return loginUser;
    }

    public String getLoginType() {
        return loginType;
    }

    public String getClientId() {
        return clientId;
    }

    public String getDevice() {
        return device;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }
}