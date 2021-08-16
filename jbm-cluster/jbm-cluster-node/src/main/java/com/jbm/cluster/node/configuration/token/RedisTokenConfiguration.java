package com.jbm.cluster.node.configuration.token;

import com.jbm.cluster.common.configuration.JbmClusterProperties;
import com.jbm.cluster.common.security.OAuthTokenType;
import com.jbm.cluster.common.security.OpenRedisTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2020-02-05 20:52
 **/
@Slf4j
@EnableConfigurationProperties({JbmClusterProperties.class})
//检查是否存redis
@ConditionalOnProperty(prefix = "jbm.cluster", name = "token-type", havingValue = "redis", matchIfMissing = false)
//@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisTokenConfiguration {

    /**
     * 构建资源服务器RedisToken服务类
     *
     * @return
     */
    @Bean
    public ResourceServerTokenServices redisTokenServices(RedisConnectionFactory redisConnectionFactory) throws Exception {
        OpenRedisTokenService tokenServices = new OpenRedisTokenService();
        // 这里的签名key 保持和认证中心一致
        RedisTokenStore redisTokenStore = new RedisTokenStore(redisConnectionFactory);
        tokenServices.setTokenStore(redisTokenStore);
        log.info("启用redis验证token[{}]", tokenServices);
        return tokenServices;
    }
}
