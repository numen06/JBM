package com.jbm.cluster.push.usage;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.model.entitys.message.MqttNotification;
import com.jbm.cluster.api.model.entitys.message.Notification;
import jbm.framework.boot.autoconfigure.fastjson.FastJsonConfiguration;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 站内消息通知
 *
 * @author wesley.zhang
 * @date 2018-3-27
 **/
@Slf4j
public class MqttNotificationExchanger implements NotificationExchanger {

    private RealMqttPahoClientFactory realMqttPahoClientFactory;

    private SimpleMqttClient mqttClient;

    public MqttNotificationExchanger(RealMqttPahoClientFactory realMqttPahoClientFactory) {
        if (realMqttPahoClientFactory != null) {
            log.info("初始化站内消息通知");
        }
        this.realMqttPahoClientFactory = realMqttPahoClientFactory;
        try {
            mqttClient = realMqttPahoClientFactory.getClientInstance(this.getClass().getSimpleName() + "_" + IdUtil.fastUUID());
            log.info("mqtt连接成功");
        } catch (Exception e) {
            log.error("mqtt连接失败", e);
        }
    }


    @Override
    public boolean support(Object notification) {
        return notification.getClass().equals(MqttNotification.class);
    }

    @Override
    public boolean exchange(Notification notification) {
        Assert.notNull(realMqttPahoClientFactory, "MQTT链接未初始化");
        MqttNotification mqttNotification = (MqttNotification) notification;
        MqttMessage message = new MqttMessage();
        if (ObjectUtil.isEmpty(mqttNotification)) {
            mqttNotification.setBody("");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HttpOutputMessage httpOutputMessage = new HttpOutputMessage() {
            @Override
            public OutputStream getBody() throws IOException {
                return outputStream;
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };
        try {
            FastJsonConfiguration.getFastJsonHttpMessageConverter().write(mqttNotification.getBody(), MediaType.APPLICATION_JSON, httpOutputMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        message.setPayload(outputStream.toByteArray());
        message.setQos(mqttNotification.getQos());
        try {
            if (StrUtil.isBlank(mqttNotification.getTopic())) {
                throw new NullPointerException("没有指定Topic");
            }
            if (!mqttClient.isConnected()) {
                mqttClient.connect();
            }
            mqttClient.publish(mqttNotification.getTopic(), message);
            log.info("发送MQTT通知成功:{}", JSON.toJSONString(notification));
        } catch (Exception e) {
            log.error("发送MQTT通知错误:{}", JSON.toJSONString(notification), e);
            return false;
        }
        return true;
    }
}
