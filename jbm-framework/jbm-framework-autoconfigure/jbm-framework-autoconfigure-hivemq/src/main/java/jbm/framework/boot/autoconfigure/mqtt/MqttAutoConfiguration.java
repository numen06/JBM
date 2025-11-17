package jbm.framework.boot.autoconfigure.mqtt;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3RxClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient;
import com.hivemq.client.mqtt.mqtt5.auth.Mqtt5EnhancedAuthMechanism;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.factories.Mqtt3ClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.factories.Mqtt5ClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.proxy.MqttProxyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nullable;

/**
 * 默认的Mqtt注入
 *
 * @author wesley
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "spring.mqtt", name = "url")
@EnableConfigurationProperties(MqttProperties.class)
public class MqttAutoConfiguration {
    @Autowired
    private MqttProperties mqttProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public RealMqttPahoClientFactory realMqttPahoClientFactory(final Mqtt5ClientFactory clientFactory) {
        mqttProperties.setAutomaticReconnect(true);
        return new RealMqttPahoClientFactory(clientFactory, mqttProperties);
    }

    @Bean
    public MqttProxyFactory mqttProxyFactory(RealMqttPahoClientFactory realMqttPahoClientFactory) {
        return new MqttProxyFactory(applicationContext, realMqttPahoClientFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.mqtt.mqtt-version", havingValue = "5", matchIfMissing = true)
    public Mqtt5ClientFactory mqtt5ClientFactory() {
        return new Mqtt5ClientFactory();
    }

    @Bean(destroyMethod = "disconnect")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.mqtt.mqtt-version", havingValue = "5", matchIfMissing = true)
    public Mqtt5AsyncClient mqtt5AsyncClient(final Mqtt5ClientFactory clientFactory, @Nullable Mqtt5EnhancedAuthMechanism enhancedAuthMechanism) {
        if (mqttProperties.getMqttVersion() == 3) {
            throw new BeanCreationException("Mqtt5AsyncClient is not available for MQTT version 5. Use Mqtt3AsyncClient instead.");
        }
        mqttProperties.setAutomaticReconnect(true);
        
        // 🔧 如果ClientId为空，自动生成一个短ClientId（避免MQTT服务器拒绝）
        if (StrUtil.isBlank(mqttProperties.getClientId())) {
            String autoClientId = "AUTO" + IdUtil.simpleUUID().substring(0, 6);
            mqttProperties.setClientId(autoClientId);
            log.warn("⚠️ MQTT ClientId not configured, auto-generated: {} (Please configure spring.mqtt.client-id in production)", autoClientId);
        }
        
        return clientFactory.mqttClient(mqttProperties, enhancedAuthMechanism);
    }

    @Bean(destroyMethod = "disconnect")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.mqtt.mqtt-version", havingValue = "5", matchIfMissing = true)
    public Mqtt5RxClient mqtt5RxClient(final Mqtt5ClientFactory clientFactory, @Nullable Mqtt5EnhancedAuthMechanism enhancedAuthMechanism) {
        return mqtt5AsyncClient(clientFactory, enhancedAuthMechanism).toRx();
    }

    @Bean(destroyMethod = "disconnect")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.mqtt.mqtt-version", havingValue = "5", matchIfMissing = true)
    public Mqtt5BlockingClient mqtt5BlockingClient(final Mqtt5ClientFactory clientFactory, @Nullable Mqtt5EnhancedAuthMechanism enhancedAuthMechanism) {
        return mqtt5AsyncClient(clientFactory, enhancedAuthMechanism).toBlocking();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.mqtt.mqtt-version", havingValue = "3")
    public Mqtt3ClientFactory mqtt3ClientFactory() {
        return new Mqtt3ClientFactory();
    }

    @Bean(destroyMethod = "disconnect")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.mqtt.mqtt-version", havingValue = "3")
    public Mqtt3AsyncClient mqtt3AsyncClient(final Mqtt3ClientFactory clientFactory) {
        if (mqttProperties.getMqttVersion() == 5) {
            throw new BeanCreationException("Mqtt3AsyncClient is not available for MQTT version 5. Use Mqtt5AsyncClient instead.");
        }
        mqttProperties.setAutomaticReconnect(true);
        
        // 🔧 如果ClientId为空，自动生成一个短ClientId（MQTT 3.1.1限制23字符）
        if (StrUtil.isBlank(mqttProperties.getClientId())) {
            String autoClientId = "AUTO" + IdUtil.simpleUUID().substring(0, 6);
            mqttProperties.setClientId(autoClientId);
            log.warn("⚠️ MQTT ClientId not configured, auto-generated: {} (Please configure spring.mqtt.client-id in production)", autoClientId);
        }
        
        return clientFactory.mqttClient(mqttProperties);
    }

    @Bean(destroyMethod = "disconnect")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.mqtt.mqtt-version", havingValue = "3")
    public Mqtt3RxClient mqtt3RxClient(final Mqtt3ClientFactory clientFactory) {
        return mqtt3AsyncClient(clientFactory).toRx();
    }

    @Bean(destroyMethod = "disconnect")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.mqtt.mqtt-version", havingValue = "3")
    public Mqtt3BlockingClient mqtt3BlockingClient(final Mqtt3ClientFactory clientFactory) {
        return mqtt3AsyncClient(clientFactory).toBlocking();
    }
}
