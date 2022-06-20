package com.jbm.cluster.common.oauth.client;

import lombok.Data;

import java.io.Serializable;

/**
 * 社交第三方账号客户端
 *
 * @author: wesley.zhang
 * @date: 2019/2/14 14:56
 * @description:
 */
@Data
public class JbmOAuth2ClientDetails implements Serializable {
    private static final long serialVersionUID = -6103012432819993075L;
    /**
     * 客户端ID
     */
    private String clientId;
    /**
     * 客户端密钥
     */
    private String clientSecret;
    /**
     * 客户端授权范围
     */
    private String scope;
    /**
     * 获取token
     */
    private String accessTokenUri;
    /**
     * 认证地址
     */
    private String userAuthorizationUri;

    /**
     * 重定向地址
     */
    private String redirectUri;

    /**
     * 获取用户信息
     */
    private String userInfoUri;

    /**
     * 登录成功地址
     */
    private String loginSuccessUri;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
