package com.jbm.cluster.push.usage;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.jbm.cluster.api.constants.push.PushWay;
import com.jbm.cluster.api.entitys.message.MqttNotification;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.model.push.PushCallback;
import com.jbm.cluster.api.model.push.PushMessageResult;
import com.jbm.util.FastJsonUtils;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttAsyncClient;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
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
public class MqttNotificationExchanger extends BaseNotificationExchanger<MqttNotification> {

    private static final long MQTT_PUBLISH_TIMEOUT_MS = 10_000L;

    private RealMqttPahoClientFactory realMqttPahoClientFactory;

    private SimpleMqttAsyncClient mqttAsyncClient;


    public MqttNotificationExchanger(RealMqttPahoClientFactory realMqttPahoClientFactory) {
        if (realMqttPahoClientFactory != null) {
            log.info("初始化站内消息通知");
        }
        this.realMqttPahoClientFactory = realMqttPahoClientFactory;
        try {
            // 使用极短Client ID：PUSH + 6位UUID（总长度11字符，符合最严格的MQTT限制）
            String shortClientId = "PUSH" + IdUtil.simpleUUID().substring(0, 6);
            mqttAsyncClient = realMqttPahoClientFactory.getAsyncClientInstance(shortClientId);
            log.info("MQTT通知异步客户端初始化成功, ClientId={}", shortClientId);
        } catch (Exception e) {
            log.error("MQTT通知客户端初始化失败", e);
        }
    }

    @Autowired(required = false)
    private FastJsonHttpMessageConverter fastJsonHttpMessageConverter;

    @Override
    public PushCallback apply(MqttNotification mqttNotification) {
        Assert.notNull(realMqttPahoClientFactory, "MQTT链接未初始化");
        Assert.notNull(mqttAsyncClient, "MQTT客户端未初始化");
        Assert.notNull(mqttNotification, "mqttNotification");
        MqttMessage message = new MqttMessage();
        if (ObjectUtil.isEmpty(mqttNotification.getBody())) {
            mqttNotification.setBody("");
        }
        message.setPayload(JSON.toJSONBytes(mqttNotification.getBody(), FastJsonUtils.defaultWebConfig()));
        message.setQos(mqttNotification.getQos());
        if (StrUtil.isBlank(mqttNotification.getTopic())) {
            throw new NullPointerException("没有指定Topic");
        }
        try {
            IMqttDeliveryToken token = mqttAsyncClient.getClient().publish(mqttNotification.getTopic(), message);
            token.waitForCompletion(MQTT_PUBLISH_TIMEOUT_MS);
        } catch (MqttException e) {
            throw new RuntimeException("MQTT发布失败或超时: " + mqttNotification.getTopic(), e);
        }
        PushCallback pushCallback = this.success(mqttNotification);
        pushCallback.setPushWay(PushWay.mqtt);
        return pushCallback;
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
