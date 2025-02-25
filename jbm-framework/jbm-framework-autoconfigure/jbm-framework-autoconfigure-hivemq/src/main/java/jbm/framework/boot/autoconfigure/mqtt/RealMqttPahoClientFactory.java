package jbm.framework.boot.autoconfigure.mqtt;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.jbm.util.BeanUtils;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.factories.Mqtt5ClientFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wesley
 */
@Slf4j
public class RealMqttPahoClientFactory {

    private final Mqtt5ClientFactory mqtt5ClientFactory;

    private final MqttProperties mqttConnectProperties;


    public RealMqttPahoClientFactory(Mqtt5ClientFactory mqtt5ClientFactory, MqttProperties mqttConnectProperties) {
        super();
        this.mqtt5ClientFactory = mqtt5ClientFactory;
        this.mqttConnectProperties = mqttConnectProperties;
    }

    @SneakyThrows
    public SimpleMqttClient getClientInstance() {
        // 创建客户端
        Mqtt5AsyncClient mqtt5AsyncClient = mqtt5ClientFactory.mqttClient(mqttConnectProperties, null);
        return new SimpleMqttClient(mqtt5AsyncClient, mqttConnectProperties);
    }

    @SneakyThrows
    public SimpleMqttClient getClientInstance(String clientId) {
        MqttProperties properties = BeanUtils.cloneJavaBean(mqttConnectProperties);
        properties.setClientId(clientId);
        Mqtt5AsyncClient mqtt5AsyncClient = mqtt5ClientFactory.mqttClient(properties, null);
        return new SimpleMqttClient(mqtt5AsyncClient, mqttConnectProperties);
    }

    @SneakyThrows
    public Mqtt5Client getClientInstance(String url, String clientId) {
        MqttProperties properties = BeanUtils.cloneJavaBean(mqttConnectProperties);
        if (StrUtil.isNotBlank(url)) {
            properties.setUrl(URLUtil.getStringURI(url));
        }
        if (StrUtil.isNotBlank(clientId)) {
            properties.setClientId(clientId);
        }
        return this.getClientInstance(properties);
    }

    /**
     * 通过参数直接创建MQTT对象
     *
     * @param properties
     * @return
     * @
     */
    public synchronized Mqtt5Client getClientInstance(MqttProperties properties) {
        // 创建客户端
        return mqtt5ClientFactory.mqttClient(properties, null);
    }

}
