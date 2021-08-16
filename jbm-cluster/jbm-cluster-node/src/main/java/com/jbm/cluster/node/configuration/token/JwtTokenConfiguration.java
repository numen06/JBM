package com.jbm.cluster.node.configuration.token;

import com.jbm.cluster.common.configuration.JbmClusterProperties;
import com.jbm.cluster.common.security.OpenJwtAccessTokenEnhancer;
import com.jbm.cluster.common.security.OpenJwtTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2020-02-05 20:52
 **/
@Slf4j
@EnableConfigurationProperties({JbmClusterProperties.class})
//检查是否存redis
@ConditionalOnProperty(prefix = "jbm.cluster", name = "token-type", havingValue = "jwt", matchIfMissing = false)
@ConditionalOnBean(RedisConnectionFactory.class)
public class JwtTokenConfiguration {

    @Autowired
    private JbmClusterProperties properties;

    @Autowired
    private DefaultAccessTokenConverter accessTokenConverter;

    /**
     * 构建jwtToken转换器
     *
     * @param properties
     * @return
     */
    public JwtAccessTokenConverter buildJwtTokenEnhancer(JbmClusterProperties properties) throws Exception {
        JwtAccessTokenConverter converter = new OpenJwtAccessTokenEnhancer();
        converter.setSigningKey(properties.getJwtSigningKey());
        converter.afterPropertiesSet();
        return converter;
    }

    /**
     * 构建资源服务器JwtToken服务类
     *
     * @return
     */
    @Bean
    public ResourceServerTokenServices jwtTokenServices() throws Exception {
        // 使用自定义系统用户凭证转换器
        OpenJwtTokenService tokenServices = new OpenJwtTokenService();
        // 这里的签名key 保持和认证中心一致
        JwtAccessTokenConverter converter = buildJwtTokenEnhancer(properties);
        JwtTokenStore jwtTokenStore = new JwtTokenStore(converter);
        tokenServices.setTokenStore(jwtTokenStore);
        tokenServices.setJwtAccessTokenConverter(converter);
        tokenServices.setDefaultAccessTokenConverter(accessTokenConverter);
        log.info("启用远程JWT验证token[{}]", tokenServices);
        return tokenServices;
    }


}
