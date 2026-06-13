package com.jbm.cluster.common.mysql.configuration;

import com.jbm.autoconfig.dic.DictionaryTemplate;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.basic.configuration.resources.JbmApiResourceScan;
import com.jbm.cluster.common.basic.configuration.resources.JbmClusterBusinessEventScan;
import com.jbm.cluster.common.basic.configuration.resources.JbmClusterDicScan;
import com.jbm.cluster.common.basic.configuration.resources.JbmClusterJobScan;
import com.jbm.cluster.common.basic.module.JbmBusinessLogTemplate;
import com.jbm.cluster.common.basic.module.JbmClusterBusinessEventTemplate;
import com.jbm.cluster.common.basic.module.JbmClusterNotification;
import com.jbm.cluster.common.basic.module.JbmClusterStreamTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DB tier bus and stream helpers. These beans depend on EventBus/Stream and are
 * intentionally kept out of the basic tier.
 */
@Slf4j
@Configuration
public class JbmMysqlBusConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JbmClusterNotification jbmClusterNotification() {
        return new JbmClusterNotification();
    }

    @Bean
    @ConditionalOnMissingBean
    public JbmApiResourceScan jbmApiResourceScan() {
        return new JbmApiResourceScan();
    }

    @Bean
    @ConditionalOnMissingBean
    public JbmClusterJobScan jbmClusterJobScan() {
        return new JbmClusterJobScan();
    }

    @Bean
    @ConditionalOnMissingBean
    public JbmClusterTemplate jbmClusterTemplate() {
        return new JbmClusterTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    public JbmClusterBusinessEventScan jbmClusterBusinessEventScan() {
        return new JbmClusterBusinessEventScan();
    }

    @Bean
    @ConditionalOnMissingBean
    public JbmClusterStreamTemplate jbmClusterStreamTemplate() {
        return new JbmClusterStreamTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    public JbmClusterBusinessEventTemplate jbmClusterBusinessEventTemplate() {
        return new JbmClusterBusinessEventTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
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
    @ConditionalOnMissingBean
    public JbmClusterDicScan clusterDicScan(DictionaryTemplate dictionaryTemplate) {
        return new JbmClusterDicScan(dictionaryTemplate);
    }
}
