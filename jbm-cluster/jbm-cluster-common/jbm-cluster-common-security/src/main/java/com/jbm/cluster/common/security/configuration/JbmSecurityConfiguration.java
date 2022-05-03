package com.jbm.cluster.common.security.configuration;

import com.jbm.cluster.common.basic.configuration.config.JbmApiScanProperties;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @Created wesley.zhang
 * @Date 2022/4/27 2:46
 * @Description TODO
 */
@Configuration
@EnableConfigurationProperties({JbmClusterProperties.class, JbmApiScanProperties.class})
public class JbmSecurityConfiguration {
    @Autowired
    private JbmClusterProperties jbmClusterProperties;
    @Autowired
    private JbmApiScanProperties jbmApiScanProperties;
//    @Autowired
//    private BusProperties busProperties;

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder jbmClusterNotification() {
        return SecurityUtils.getPasswordEncoder();
    }


}
