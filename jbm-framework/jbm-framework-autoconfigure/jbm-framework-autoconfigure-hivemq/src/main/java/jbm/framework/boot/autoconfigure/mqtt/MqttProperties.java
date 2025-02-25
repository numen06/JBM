package jbm.framework.boot.autoconfigure.mqtt;

import jbm.framework.boot.autoconfigure.mqtt.hivemq.config.HiveMqttProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wesley
 */
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "spring.mqtt")
@Data
public class MqttProperties extends HiveMqttProperties {

//    public MqttConnectOptions toMqttConnectOptions() {
//        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
//        mqttConnectOptions.setServerURIs(StrUtil.splitToArray(this.getUrl(), ","));
//        mqttConnectOptions.setUserName(this.getUsername());
//        if (StrUtil.isNotBlank(this.getPassword())) {
//            mqttConnectOptions.setPassword(this.getPassword().toCharArray());
//        }
//        mqttConnectOptions.setConnectionTimeout(this.getConnectionTimeout());
//        mqttConnectOptions.setKeepAliveInterval(this.getKeepAliveInterval());
//        mqttConnectOptions.setAutomaticReconnect(this.getAutomaticReconnect());
//        mqttConnectOptions.setCleanSession(this.getCleanSession());
//        mqttConnectOptions.setMaxInflight(this.getMaxInflight());
//        return mqttConnectOptions;
//    }


//    public MqttConnectProperties fromMqttConnectOptions(MqttConnectOptions mqttConnectOptions) {
//        if (ObjectUtil.isNotEmpty(mqttConnectOptions.getServerURIs())) {
//            this.setUrl(StrUtil.join(",", mqttConnectOptions.getServerURIs()));
//        }
//        this.setUsername(mqttConnectOptions.getUserName());
//        this.setPassword(StrUtil.str(mqttConnectOptions.getPassword(), StandardCharsets.UTF_8));
//        this.setConnectionTimeout(mqttConnectOptions.getConnectionTimeout());
//        this.setKeepAliveInterval(mqttConnectOptions.getKeepAliveInterval());
//        this.setAutomaticReconnect(mqttConnectOptions.isAutomaticReconnect());
//        this.setCleanSession(mqttConnectOptions.isCleanSession());
//        if (mqttConnectOptions.getMaxInflight() >= 1000) {
//            this.setMqttVersion(mqttConnectOptions.getMqttVersion());
//        } else {
//            this.setMaxInflight(1000);
//        }
//
//        return this;
//    }


}
