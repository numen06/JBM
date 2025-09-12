package jbm.framework.boot.autoconfigure.emqx.configuration;


import jbm.framework.boot.autoconfigure.emqx.EmqxApiListener;
import jbm.framework.boot.autoconfigure.emqx.EmqxApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import javax.annotation.Resource;

/**
 * @author wesley
 */
@Slf4j
@EnableConfigurationProperties({EmqxProperties.class, EmqxMqttProperties.class})
@ConditionalOnProperty(prefix = "emqx.api", name = "url")
public class EmqxConfiguration {

    @Resource
    private EmqxProperties emqxProperties;

    @Resource
    private EmqxMqttProperties emqxMqttProperties;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Bean
    public EmqxApiService getEmqxApiService() {
        return new EmqxApiService(emqxProperties);
    }

    @Bean
    public EmqxApiListener getEmqxApiClientService() {
        return new EmqxApiListener(emqxMqttProperties, eventPublisher);
    }


}
