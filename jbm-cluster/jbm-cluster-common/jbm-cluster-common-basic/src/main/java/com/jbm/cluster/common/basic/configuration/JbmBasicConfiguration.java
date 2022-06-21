package com.jbm.cluster.common.basic.configuration;

import com.jbm.cluster.common.basic.module.JbmClusterStreamTemplate;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.basic.module.JbmClusterNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Created wesley.zhang
 * @Date 2022/4/27 2:46
 * @Description TODO
 */
@EnableConfigurationProperties({JbmClusterProperties.class})
public class JbmBasicConfiguration {
    @Autowired
    private JbmClusterProperties jbmClusterProperties;
//    @Autowired
//    private BusProperties busProperties;

    @Bean
    public JbmClusterNotification jbmClusterNotification() {
        return new JbmClusterNotification();
    }

    @Bean
    public RequestMappingScan requestMappingScan() {
        return new RequestMappingScan(jbmClusterProperties);
    }

    @Bean
    public JbmClusterTemplate jbmClusterTemplate() {
        return new JbmClusterTemplate();
    }

    @Bean
    public JbmClusterStreamTemplate jbmClusterStreamTemplate() {
        JbmClusterStreamTemplate jbmClusterStreamTemplate = new JbmClusterStreamTemplate();
        return jbmClusterStreamTemplate;
    }

    @Bean
    public BasicUnknownRuntimeExceptionFilter basicUnknownRuntimeExceptionFilter() {
        return new BasicUnknownRuntimeExceptionFilter();
    }


}
