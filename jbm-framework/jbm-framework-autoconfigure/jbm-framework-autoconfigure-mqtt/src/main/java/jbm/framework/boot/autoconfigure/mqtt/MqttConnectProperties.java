package jbm.framework.boot.autoconfigure.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.net.SocketFactory;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.mqtt")
@Data
public class MqttConnectProperties {

    private Integer keepAliveInterval = MqttConnectOptions.KEEP_ALIVE_INTERVAL_DEFAULT;
    private Integer maxInflight = MqttConnectOptions.MAX_INFLIGHT_DEFAULT;
    private String willDestination = null;
    private String username = MqttClient.generateClientId();
    private String password = "";
    private SocketFactory socketFactory;
    private Properties sslClientProps = null;
    private Boolean cleanSession = MqttConnectOptions.CLEAN_SESSION_DEFAULT;
    private Integer connectionTimeout = MqttConnectOptions.CONNECTION_TIMEOUT_DEFAULT;
    private String url = null;
    private Integer MqttVersion = MqttConnectOptions.MQTT_VERSION_DEFAULT;
    private Boolean automaticReconnect = true;
    private String clientId = MqttClient.generateClientId();

    public MqttConnectOptions toMqttConnectOptions() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setServerURIs(StrUtil.splitToArray(this.getUrl(), ","));
        mqttConnectOptions.setUserName(this.getUsername());
        mqttConnectOptions.setPassword(this.getPassword().toCharArray());
        mqttConnectOptions.setConnectionTimeout(this.getConnectionTimeout());
        mqttConnectOptions.setKeepAliveInterval(this.getKeepAliveInterval());
        mqttConnectOptions.setAutomaticReconnect(this.getAutomaticReconnect());
        mqttConnectOptions.setCleanSession(this.getCleanSession());
        return mqttConnectOptions;
    }


    public MqttConnectProperties fromMqttConnectOptions(MqttConnectOptions mqttConnectOptions) {
        this.setUrl(StrUtil.join(",", mqttConnectOptions.getServerURIs()));
        this.setUsername(mqttConnectOptions.getUserName());
        this.setPassword(StrUtil.str(mqttConnectOptions.getPassword(), StandardCharsets.UTF_8));
        this.setConnectionTimeout(mqttConnectOptions.getConnectionTimeout());
        this.setKeepAliveInterval(mqttConnectOptions.getKeepAliveInterval());
        this.setAutomaticReconnect(mqttConnectOptions.isAutomaticReconnect());
        this.setCleanSession(mqttConnectOptions.isCleanSession());
        return this;
    }


}
