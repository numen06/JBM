package com.jbm.cluster.common.configuration;

import com.jbm.cluster.common.security.OAuthTokenType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自定义网关配置
 *
 * @author: wesley.zhang
 * @date: 2018/11/23 14:40
 * @description:
 */
@Data
@ConfigurationProperties(prefix = "jbm.cluster")
public class JbmClusterProperties {

    /**
     * token验证类型
     */
    private OAuthTokenType tokenType;

    /**
     * 网关服务地址
     */
    private String apiServerAddr;

    /**
     * 平台认证服务地址
     */
    private String authServerAddr;

    /**
     * 后台部署地址
     */
    private String adminServerAddr;

    /**
     * 网关客户端Id
     */
    private String clientId;
    /**
     * 网关客户端密钥
     */
    private String clientSecret;

    /**
     * 认证范围
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
     * 获取token地址
     */
    private String tokenInfoUri;
    /**
     * 获取用户信息地址
     */
    private String userInfoUri;

    /**
     * jwt签名key
     */
    private String jwtSigningKey;

    /**
     * 忽略验证路径
     */
    private String[] permitAll = new String[]{};

}
