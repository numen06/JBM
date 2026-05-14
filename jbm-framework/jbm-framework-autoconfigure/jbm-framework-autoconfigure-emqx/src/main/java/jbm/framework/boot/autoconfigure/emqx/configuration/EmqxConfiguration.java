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
 * EMQX 自动配置：仅提供 API 与 MQTT 监听等能力，不提供 REST 钩子端点。
 * 使用方（如 tpm）自行提供 REST 接口后，调用本模块的 Handler 接口与事件完成“REST 之后的处理”。
 */
@Slf4j
@EnableConfigurationProperties({EmqxProperties.class, EmqxMqttProperties.class})
public class EmqxConfiguration {

    @Resource
    private EmqxProperties emqxProperties;

    @Resource
    private EmqxMqttProperties emqxMqttProperties;

    @Bean
    @ConditionalOnProperty(prefix = "emqx.api", name = "url")
    public EmqxApiService getEmqxApiService() {
        return new EmqxApiService(emqxProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "emqx.mqtt", name = "url")
    public EmqxApiListener getEmqxApiClientService(ApplicationEventPublisher applicationEventPublisher) {
        return new EmqxApiListener(emqxMqttProperties, applicationEventPublisher);
    }
}
