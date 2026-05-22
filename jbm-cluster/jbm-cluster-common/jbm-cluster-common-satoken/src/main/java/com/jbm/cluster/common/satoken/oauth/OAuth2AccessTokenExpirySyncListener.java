package com.jbm.cluster.common.satoken.oauth;

import cn.dev33.satoken.listener.SaTokenListenerForSimple;
import cn.dev33.satoken.stp.SaLoginModel;

public class OAuth2AccessTokenExpirySyncListener extends SaTokenListenerForSimple {

    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        AccessTokenExpiryAligner.tryResyncOAuth2AccessToken(tokenValue);
    }

    @Override
    public void doRenewTimeout(String tokenValue, Object loginId, long timeout) {
        AccessTokenExpiryAligner.tryResyncOAuth2AccessToken(tokenValue);
    }
}