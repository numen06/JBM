package com.jbm.test.mqtt;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSONArray;
import jbm.framework.boot.autoconfigure.mqtt.MqttProperties;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.factories.Mqtt5ClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.proxy.MqttCallProxyFactory;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;

@Slf4j
public class MqttCallClientTest {

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
    public void testCallJsonObj() {
        com.jbm.test.mqtt.call.MqttCallService mqttCallClient = mqttCallProxyFactory.getClient(com.jbm.test.mqtt.call.MqttCallService.class);
        Object body = mqttCallClient.call("{\"msg\":\"Hello\", \"id\":123, \"extra\":\"ignored\"}");
        log.info("mqtt call json obj result: {}", body);
    }
    @Test
    public void testCallJsonArray() {
        com.jbm.test.mqtt.call.MqttCallService mqttCallClient = mqttCallProxyFactory.getClient(com.jbm.test.mqtt.call.MqttCallService.class);
        JSONArray params = new JSONArray();
        params.add("Hello");
        Object body = mqttCallClient.callArray(params);
        log.info("mqtt call json array result: {}", body);
        ThreadUtil.safeSleep(1000);
    }


    @Test
    public void testCallMap() {
        com.jbm.test.mqtt.call.MqttCallService mqttCallClient = mqttCallProxyFactory.getClient(com.jbm.test.mqtt.call.MqttCallService.class);
        Object body = mqttCallClient.callMap(MapUtil.of("msg", "Hello"));
        log.info("mqtt call map result: {}", body);
//        ThreadUtil.safeSleep(1000);
    }

    @Test
    public void testCallList() {
        com.jbm.test.mqtt.call.MqttCallService mqttCallClient = mqttCallProxyFactory.getClient(com.jbm.test.mqtt.call.MqttCallService.class);
        Object body = mqttCallClient.callList(Lists.newArrayList("Hello", "World"));
        log.info("mqtt call list result: {}", body);
//        ThreadUtil.safeSleep(1000);
    }
}
