package com.jbm.cluster.common.basic.configuration;

import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.basic.module.JbmRequestTemplate;
import com.jbm.cluster.common.basic.module.request.JbmHttpRequest;
import com.jbm.cluster.common.basic.module.request.JbmHttpsRequest;
import com.jbm.cluster.common.basic.runtime.BasicUnknownRuntimeExceptionFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author wesley
 * @Created wesley.zhang
 * @Date 2022/4/27 2:46
 * @Description TODO
 */
@Slf4j
@EnableConfigurationProperties({JbmClusterProperties.class})
public class JbmBasicConfiguration {
    @Autowired
    private JbmClusterProperties jbmClusterProperties;
//    @Autowired
//    private BusProperties busProperties;

    @Bean
    public BasicUnknownRuntimeExceptionFilter basicUnknownRuntimeExceptionFilter() {
        return new BasicUnknownRuntimeExceptionFilter();
    }

    @Bean
    public JbmRequestTemplate jbmRequestTemplate() {
        return new JbmRequestTemplate();
    }


    @Bean
    public JbmHttpRequest jbmHttpRequest() {
        return new JbmHttpRequest();
    }


    @Bean
    public JbmHttpsRequest jbmHttpsRequest() {
        return new JbmHttpsRequest();
    }


}
