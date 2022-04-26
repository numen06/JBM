package com.jbm.cluster.push.usage;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.entitys.message.Notification;
import com.jbm.cluster.api.entitys.message.PushMessage;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 站内消息通知
 *
 * @author wesley.zhang
 * @date 2018-3-27
 **/
@Slf4j
public class PushMessageNotificationExchanger   {

    private RealMqttPahoClientFactory realMqttPahoClientFactory;

    private SimpleMqttClient mqttClient;

    public PushMessageNotificationExchanger(RealMqttPahoClientFactory realMqttPahoClientFactory) {
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

//    @Override
    public boolean support(Notification notification) {
        return notification.getClass().equals(PushMessage.class);
    }

//    @Override
    public boolean exchange(PushMessage mqttNotification) {
        Assert.notNull(realMqttPahoClientFactory, "MQTT链接未初始化");
        MqttMessage message = new MqttMessage();
        message.setPayload(JSON.toJSONBytes(mqttNotification));
        String topic = "user/" + mqttNotification.getRecUserId();
        if (ObjectUtil.isEmpty(mqttNotification.getRecUserId())) {
            topic = "system/web";
        }
        try {
            mqttClient.publish(topic, message);
            if (StrUtil.isNotEmpty(mqttNotification.getTags())) {
                List<String> tags = StrUtil.split(mqttNotification.getTags(), ",");
                for (String tag : tags) {
                    mqttClient.publish("tags/" + tag, message);
                }
            }
            log.info("发送:{}成功", topic);
        } catch (Exception e) {
            log.error("发送站内信错误", e);
            return false;
        }
        return true;
    }
}
