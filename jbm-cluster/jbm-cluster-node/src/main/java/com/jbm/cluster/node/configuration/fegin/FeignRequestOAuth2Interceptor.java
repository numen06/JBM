package com.jbm.cluster.node.configuration.fegin;

import feign.RequestTemplate;
import jbm.framework.boot.autoconfigure.fegin.FeignRequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * 微服务之间feign调用请求头丢失的问题
 * 加入微服务之间传递的唯一标识,便于追踪
 *
 * @author wesley.zhang
 */
@Slf4j
public class FeignRequestOAuth2Interceptor extends FeignRequestInterceptor {
    public static final String BEARER = "Bearer";
    public static final String AUTHORIZATION = "Authorization";
    private final String tokenType = BEARER;
    private final String header = AUTHORIZATION;

    private final OAuth2RestTemplate oAuth2RestTemplate;

    public FeignRequestOAuth2Interceptor(OAuth2RestTemplate oAuth2RestTemplate) {
        this.oAuth2RestTemplate = oAuth2RestTemplate;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(this.header, new String[0]);
        template.header(this.header, new String[]{this.extract(this.tokenType)});
        super.apply(template);
    }

    protected String extract(String tokenType) {
        try {
            OAuth2AccessToken accessToken = oAuth2RestTemplate.getAccessToken();
            return String.format("%s %s", tokenType, accessToken.getValue());
        } catch (Exception e) {
            log.info("获取集群的认证错误，请检查认证服务器设置。");
        }
        return null;
    }


}
