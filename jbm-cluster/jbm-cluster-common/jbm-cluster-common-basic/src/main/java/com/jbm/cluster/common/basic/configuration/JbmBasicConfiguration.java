package com.jbm.cluster.common.basic.configuration;

import com.jbm.autoconfig.dic.DictionaryTemplate;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.basic.configuration.resources.JbmApiResourceScan;
import com.jbm.cluster.common.basic.configuration.resources.JbmClusterBusinessEventScan;
import com.jbm.cluster.common.basic.configuration.resources.JbmClusterDicScan;
import com.jbm.cluster.common.basic.configuration.resources.JbmClusterJobScan;
import com.jbm.cluster.common.basic.module.*;
import com.jbm.cluster.common.basic.module.request.JbmHttpRequest;
import com.jbm.cluster.common.basic.module.request.JbmHttpsRequest;
import com.jbm.cluster.common.basic.runtime.BasicUnknownRuntimeExceptionFilter;
import com.jbm.cluster.common.basic.service.LoginErrorMessageService;
import com.jbm.cluster.common.basic.service.SysDebugModeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
    public JbmClusterNotification jbmClusterNotification() {
        return new JbmClusterNotification();
    }

    @Bean
    public JbmApiResourceScan jbmApiResourceScan() {
        return new JbmApiResourceScan();
    }

    @Bean
    public JbmClusterJobScan jbmClusterJobScan() {
        return new JbmClusterJobScan();
    }

    @Bean
    public JbmClusterTemplate jbmClusterTemplate() {
        return new JbmClusterTemplate();
    }

    @Bean
    public JbmClusterBusinessEventScan jbmClusterBusinessEventScan() {
        return new JbmClusterBusinessEventScan();
    }

    @Bean
    public JbmClusterStreamTemplate jbmClusterStreamTemplate() {
        JbmClusterStreamTemplate jbmClusterStreamTemplate = new JbmClusterStreamTemplate();
        return jbmClusterStreamTemplate;
    }

    @Bean
    public JbmClusterBusinessEventTemplate jbmClusterBusinessEventTemplate() {
        JbmClusterBusinessEventTemplate jbmClusterBusinessEventTemplate = new JbmClusterBusinessEventTemplate();
        return jbmClusterBusinessEventTemplate;
    }

    @Bean
    public BasicUnknownRuntimeExceptionFilter basicUnknownRuntimeExceptionFilter() {
        return new BasicUnknownRuntimeExceptionFilter();
    }

    @Bean
    public JbmBusinessLogTemplate jbmBusinessLogTemplate() {
        log.info("========================================");
        log.info("注册 JbmBusinessLogTemplate Bean");
        log.info("依赖检查：");
        log.info("- BusinessLogClient (Feign): 已在 @EnableFeignClients 中配置");
        log.info("- StreamBridge (RabbitMQ): 需要 spring-cloud-stream 依赖");
        log.info("========================================");
        return new JbmBusinessLogTemplate();
    }


    @Bean
    @ConditionalOnBean(DictionaryTemplate.class)
    public JbmClusterDicScan clusterDicScan(DictionaryTemplate dictionaryTemplate) {
        JbmClusterDicScan scan = new JbmClusterDicScan(dictionaryTemplate);
        return scan;
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

    @Bean
    public SysDebugModeService sysDebugModeService() {
        return new SysDebugModeService();
    }

    @Bean
    public LoginErrorMessageService loginErrorMessageService() {
        return new LoginErrorMessageService();
    }


}
