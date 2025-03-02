package com.jbm.test.mqtt;

import cn.hutool.core.thread.ThreadUtil;
import com.jbm.test.mqtt.call.MqttCallService;
import com.jbm.test.mqtt.call.MqttCallServiceImpl;
import jbm.framework.boot.autoconfigure.mqtt.MqttProperties;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.factories.Mqtt5ClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.proxy.MqttCallProxyFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class MqttCallTest {

    private static MqttCallProxyFactory mqttCallProxyFactory;
    private static final MqttProperties mqttProperties = new MqttProperties();
    private static RealMqttPahoClientFactory realMqttPahoClientFactory;

    @BeforeAll
    public static void testClient() throws Exception {
        Mqtt5ClientFactory mqtt5ClientFactory = new Mqtt5ClientFactory();
        mqttProperties.setUrl(URI.create("tcp://www.51jbm.cn:1883"));
        realMqttPahoClientFactory = new RealMqttPahoClientFactory(mqtt5ClientFactory, mqttProperties);
        mqttCallProxyFactory = new MqttCallProxyFactory(realMqttPahoClientFactory);
    }


    @Test
    public void testCall() {
        MqttCallService mqttCallService = mqttCallProxyFactory.getService("test", MqttCallServiceImpl.class);
        String msg = "{\"msg\":\"Hello\", \"id\":123, \"time\":\"2025-05-05\"}";
        mqttCallProxyFactory.requestAndResponseEvent("test/request", "test/response", "test.call", msg, event -> {
            System.out.println(event.getMessage());
        });
    }

    @Test
    public void testIntCall() {
        MqttCallService mqttCallService = mqttCallProxyFactory.getService("test", MqttCallServiceImpl.class);
        MqttCallService mqttCallClient = mqttCallProxyFactory.getClient(MqttCallService.class);
        String body = mqttCallClient.call("{\"msg\":\"Hello\", \"id\":123, \"extra\":\"ignored\"}");
        System.out.println(body);
        ThreadUtil.safeSleep(2000);
    }
}
