package com.jbm.cluster.push.usage;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.entitys.message.MqttNotification;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.util.Assert;

/**
 * 站内消息通知
 *
 * @author wesley.zhang
 * @date 2018-3-27
 **/
@Slf4j
public class MqttNotificationExchanger extends BaseNotificationExchanger<MqttNotification> {

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
    public boolean process(MqttNotification mqttNotification) {
        Assert.notNull(realMqttPahoClientFactory, "MQTT链接未初始化");
        MqttMessage message = new MqttMessage();
        if (ObjectUtil.isEmpty(mqttNotification)) {
            mqttNotification.setBody("");
        }
        message.setPayload(JSON.toJSONBytes(mqttNotification.getBody()));
        message.setQos(mqttNotification.getQos());
        try {
            if (StrUtil.isBlank(mqttNotification.getTopic())) {
                throw new NullPointerException("没有指定Topic");
            }
            mqttClient.publish(mqttNotification.getTopic(), message);
            log.info("发送MQTT通知成功:{}", JSON.toJSONString(mqttNotification));
        } catch (Exception e) {
            log.error("发送MQTT通知错误:{}", JSON.toJSONString(mqttNotification), e);
            return false;
        }
        return true;
    }
}
