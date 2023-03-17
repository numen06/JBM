package com.jbm.cluster.node.configuration.token;

import com.jbm.cluster.common.configuration.JbmClusterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-03-04 04:12
 **/
@Slf4j
@EnableConfigurationProperties({JbmClusterProperties.class})
@ConditionalOnProperty(prefix = "jbm.cluster", name = "token-type", havingValue = "remote", matchIfMissing = false)
public class RemoteTokenConfiguration {


    @Autowired
    private JbmClusterProperties properties;

    /**
     * 构建token转换器
     *
     * @return
     */
    @Autowired
    private DefaultAccessTokenConverter accessTokenConverter;

    /**
     * 构建自定义远程Token服务类
     *
     * @param properties
     * @return
     */
    @Bean
    public RemoteTokenServices remoteTokenServices() {
        // 使用自定义系统用户凭证转换器
        RemoteTokenServices tokenServices = new RemoteTokenServices();
        tokenServices.setCheckTokenEndpointUrl(properties.getTokenInfoUri());
        tokenServices.setClientId(properties.getClientId());
        tokenServices.setClientSecret(properties.getClientSecret());
        tokenServices.setAccessTokenConverter(accessTokenConverter);
        log.info("启用远程URL验证token[{}]", tokenServices);
        return tokenServices;
    }

}
