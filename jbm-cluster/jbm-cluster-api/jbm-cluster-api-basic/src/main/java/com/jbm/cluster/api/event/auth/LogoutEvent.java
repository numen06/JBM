package com.jbm.cluster.api.event.auth;

import com.jbm.cluster.api.model.auth.JbmLoginUser;
import org.springframework.context.ApplicationEvent;

public class LogoutEvent extends ApplicationEvent {

    private final Object loginId;
    private final JbmLoginUser loginUser;
    private final String tokenValue;

    public LogoutEvent(Object source, Object loginId, JbmLoginUser loginUser, String tokenValue) {
        super(source);
        this.loginId = loginId;
        this.loginUser = loginUser;
        this.tokenValue = tokenValue;
    }

    public Object getLoginId() {
        return loginId;
    }

    public JbmLoginUser getLoginUser() {
        return loginUser;
    }

    public String getTokenValue() {
        return tokenValue;
    }
}