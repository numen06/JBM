package com.jbm.cluster.common.oauth;

import com.jbm.cluster.common.configuration.JbmClusterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.web.client.RestTemplate;

/**
 * 自定义RestTemplate请求工具类
 *
 * @author: wesley.zhang
 * @date: 2018/12/11 15:51
 * @description:
 */
@Slf4j
public class OpenRestTemplate extends RestTemplate {

    private JbmClusterProperties common;
    private ApplicationEventPublisher publisher;
    private BusProperties busProperties;

    public OpenRestTemplate(JbmClusterProperties common, BusProperties busProperties, ApplicationEventPublisher publisher) {
        this.common = common;
        this.publisher = publisher;
        this.busProperties = busProperties;
    }

    /**
     * 构建网关Oauth2 client_credentials方式请求
     *
     * @return
     */
    public OAuth2RestTemplate buildOAuth2ClientRequest() {
        log.info("获取到应用程序认证信息:[{}],URL:[{}]", common.getClientId(), common.getAccessTokenUri());
        return buildOAuth2ClientRequest(common.getClientId(), common.getClientSecret(), common.getAccessTokenUri());
    }

    /**
     * 构建网关Oauth2 client_credentials方式请求
     *
     * @param clientId
     * @param clientSecret
     * @param accessTokenUri
     * @return
     */
    public OAuth2RestTemplate buildOAuth2ClientRequest(String clientId, String clientSecret, String accessTokenUri) {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
        resource.setClientId(clientId);
        resource.setClientSecret(clientSecret);
        resource.setAccessTokenUri(accessTokenUri);
        OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resource);
        return restTemplate;
    }

    /**
     * 构建网关Oauth2 password方式请求
     *
     * @return
     */
    public OAuth2RestTemplate buildOAuth2PasswordRequest(String username, String password) {
        return buildOAuth2PasswordRequest(common.getClientId(), common.getClientSecret(), common.getAccessTokenUri(), username, password);
    }

    /**
     * 构建网关Oauth2 password方式请求
     *
     * @param clientId
     * @param clientSecret
     * @param accessTokenUri
     * @param username
     * @param password
     * @return
     */
    public OAuth2RestTemplate buildOAuth2PasswordRequest(String clientId, String clientSecret, String accessTokenUri, String username, String password) {
        ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
        resource.setUsername(username);
        resource.setPassword(password);
        resource.setClientId(clientId);
        resource.setClientSecret(clientSecret);
        resource.setAccessTokenUri(accessTokenUri);
        resource.setAuthenticationScheme(AuthenticationScheme.form);
        resource.setGrantType("password");
        OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resource);
        return restTemplate;
    }

}
