package jbm.framework.boot.autoconfigure.mqtt;

import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.proxy.MqttProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 默认的Mqtt注入
 *
 * @author wesley
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.mqtt", name = "url")
@EnableConfigurationProperties(MqttConnectProperties.class)
public class MqttAutoConfiguration {
    @Autowired
    private MqttConnectProperties mqttConnectProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public RealMqttPahoClientFactory realMqttPahoClientFactory() {
        RealMqttPahoClientFactory factory = new RealMqttPahoClientFactory(mqttConnectProperties);
        return factory;
    }

    @Bean
    public SimpleMqttClient simpleMqttClient(RealMqttPahoClientFactory realMqttPahoClientFactory) {
        return realMqttPahoClientFactory.getClientInstance();
    }

    @Bean
    public MqttProxyFactory mqttProxyFactory(RealMqttPahoClientFactory realMqttPahoClientFactory) {
        return new MqttProxyFactory(applicationContext, realMqttPahoClientFactory);
    }

}
