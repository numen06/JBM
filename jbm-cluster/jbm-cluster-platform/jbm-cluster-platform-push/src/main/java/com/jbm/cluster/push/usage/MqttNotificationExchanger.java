package com.jbm.cluster.push.usage;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.jbm.cluster.api.entitys.message.MqttNotification;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.model.push.PushCallback;
import com.jbm.cluster.api.model.push.PushMessageResult;
import com.jbm.util.FastJsonUtils;
import jbm.framework.boot.autoconfigure.amqp.usage.FastJsonMessageConverter;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;

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

    @Autowired(required = false)
    private FastJsonHttpMessageConverter fastJsonHttpMessageConverter;

    @SneakyThrows
    @Override
    public PushCallback apply(MqttNotification mqttNotification) {
        Assert.notNull(realMqttPahoClientFactory, "MQTT链接未初始化");
        MqttMessage message = new MqttMessage();
        if (ObjectUtil.isEmpty(mqttNotification)) {
            mqttNotification.setBody("");
        }
        message.setPayload(JSON.toJSONBytes(mqttNotification.getBody(),FastJsonUtils.defaultWebConfig()));
        message.setQos(mqttNotification.getQos());
        if (StrUtil.isBlank(mqttNotification.getTopic())) {
            throw new NullPointerException("没有指定Topic");
        }
        mqttClient.publish(mqttNotification.getTopic(), message);
//            log.info("发送MQTT通知成功:{}", JSON.toJSONString(mqttNotification));
        return this.success(mqttNotification);
    }


    @Override
    public MqttNotification build(PushMessageBody pushMessageBody, PushMessageItem pushMessageItem) {
        MqttNotification mqttNotification = new MqttNotification();
        String topic = "user/" + pushMessageItem.getRecUserId();
        mqttNotification.setTopic(topic);
        mqttNotification.setMsgId(pushMessageItem.getMsgId());
        mqttNotification.setPushStatus(pushMessageItem.getPushStatus());
        PushMessageResult pushMessageResult = new PushMessageResult();
        BeanUtil.copyProperties(pushMessageItem, pushMessageResult);
        BeanUtil.copyProperties(pushMessageBody, pushMessageResult);
        mqttNotification.setBody(pushMessageResult);
        return mqttNotification;
    }


}
