package com.jbm.cluster.push.usage;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.model.entity.message.PushMessage;
import com.jbm.cluster.api.model.message.MqttNotification;
import com.jbm.cluster.api.model.message.Notification;
import com.jbm.cluster.api.model.message.SmsNotification;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * 站内消息通知
 *
 * @author wesley.zhang
 * @date 2018-3-27
 **/
@Slf4j
public class MqttNotificationExchanger implements NotificationExchanger {

    private RealMqttPahoClientFactory realMqttPahoClientFactory;

    private IMqttClient mqttClient;

    public MqttNotificationExchanger(RealMqttPahoClientFactory realMqttPahoClientFactory) {
        if (realMqttPahoClientFactory != null) {
            log.info("初始化站内消息通知");
        }
        this.realMqttPahoClientFactory = realMqttPahoClientFactory;
        try {
            mqttClient = realMqttPahoClientFactory.getClientInstance("jbm_push_" + IdUtil.fastUUID());
            mqttClient.connect();
            log.info("mqtt连接成功");
        } catch (Exception e) {
            log.error("mqtt连接失败", e);
        }
    }

    @Override
    public boolean support(Object notification) {
        return notification.getClass().equals(PushMessage.class);
    }

    @Override
    public boolean exchange(Notification notification) {
        Assert.notNull(realMqttPahoClientFactory, "MQTT链接未初始化");
        PushMessage mqttNotification = (PushMessage) notification;
        MqttMessage message = new MqttMessage();
        message.setPayload(JSON.toJSONBytes(notification));
        String topic = "user/" + mqttNotification.getRecUserId();
        try {
            mqttClient.publish(topic, message);
            log.info("发送:{}成功", topic);
        } catch (Exception e) {
            log.error("发送站内信错误", e);
            return false;
        }
        return true;
    }
}
