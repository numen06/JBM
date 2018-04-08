package jbm.framework.boot.autoconfigure.mqtt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 默认的Mongo注入
 * 
 * @author wesley
 *
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.mqtt", name = "url")
@EnableConfigurationProperties(MqttConnectProperties.class)
public class MqttAutoConfiguration {
	@Autowired
	private MqttConnectProperties mqttConnectProperties;

	@Bean
	public RealMqttPahoClientFactory realMqttClientFactory() {
		RealMqttPahoClientFactory factory = new RealMqttPahoClientFactory(mqttConnectProperties);
		return factory;
	}

}
