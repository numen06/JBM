package com.jbm.cluster.api.event.auth;

import org.springframework.context.ApplicationEvent;

public class LoginFailureEvent extends ApplicationEvent {

    private final String username;
    private final String loginType;
    private final String clientId;
    private final String ip;
    private final String reason;

    public LoginFailureEvent(Object source, String username, String loginType, String clientId,
                             String ip, String reason) {
        super(source);
        this.username = username;
        this.loginType = loginType;
        this.clientId = clientId;
        this.ip = ip;
        this.reason = reason;
    }

    public String getUsername() {
        return username;
    }

    public String getLoginType() {
        return loginType;
    }

    public String getClientId() {
        return clientId;
    }

    public String getIp() {
        return ip;
    }

    public String getReason() {
        return reason;
    }
}