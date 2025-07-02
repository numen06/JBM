package com.jbm.test.mqtt;

import cn.hutool.core.thread.ThreadUtil;
import com.jbm.test.mqtt.call.MqttCallServiceImpl;
import jbm.framework.boot.autoconfigure.mqtt.MqttProperties;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.factories.Mqtt5ClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.proxy.MqttCallProxyFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;

@Slf4j
public class MqttCallServiceTest {

    private static final MqttProperties mqttProperties = new MqttProperties();
    private static MqttCallProxyFactory mqttCallProxyFactory;
    private static RealMqttPahoClientFactory realMqttPahoClientFactory;

    private static com.jbm.test.mqtt.call.MqttCallService mqttCallService;

    @BeforeAll
    public static void testClient() throws Exception {
        Mqtt5ClientFactory mqtt5ClientFactory = new Mqtt5ClientFactory();
        mqttProperties.setUrl(URI.create("tcp://10.100.10.121:1883"));
        realMqttPahoClientFactory = new RealMqttPahoClientFactory(mqtt5ClientFactory, mqttProperties);
        mqttCallProxyFactory = new MqttCallProxyFactory(realMqttPahoClientFactory);
        mqttCallService = mqttCallProxyFactory.getService("test", MqttCallServiceImpl.class);
    }


    @Test
    public void testCall() {
        String msg = "{\"msg\":\"Hello\", \"id\":123, \"time\":\"2025-05-05\"}";
        mqttCallProxyFactory.requestAndResponseEvent("test/request", "test/response", "test.call", msg, event -> {
            Object body = event.getMessage();
            log.info("mqtt call result: {}", body);
        });
        ThreadUtil.waitForDie();
    }
    @Test
    public void testSub() {
        realMqttPahoClientFactory.getClientInstance("test").subscribe("testtopic/#", event -> {
            log.info("mqtt call result: {}", event.getTopic() );
        });
        ThreadUtil.waitForDie();
    }


}
