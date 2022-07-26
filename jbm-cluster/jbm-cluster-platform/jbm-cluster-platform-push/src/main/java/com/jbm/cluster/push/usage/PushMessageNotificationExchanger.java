package com.jbm.cluster.push.usage;

import com.jbm.cluster.api.entitys.message.MqttNotification;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.common.basic.module.JbmClusterNotification;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collection;
import java.util.Map;

/**
 * 站内消息通知
 *
 * @author wesley.zhang
 * @date 2018-3-27
 **/
@Slf4j
public class PushMessageNotificationExchanger implements ApplicationContextAware {
    private Collection<NotificationExchanger> exchangers;

    @Autowired
    private MqttNotificationExchanger mqttNotificationExchanger;

    @Autowired
    private JbmClusterNotification jbmClusterNotification;

    private SimpleMqttClient mqttClient;

    public PushMessageNotificationExchanger() {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, NotificationExchanger> beansOfType = applicationContext.getBeansOfType(NotificationExchanger.class);
        this.exchangers = beansOfType.values();
    }


    //    @Override
    public boolean exchange(PushMessageBody pushMessageBody, PushMessageItem pushMessageItem) {
        switch (pushMessageItem.getPushWay()) {
            case mqtt:
                MqttNotification mqttNotification = mqttNotificationExchanger.build(pushMessageBody, pushMessageItem);
                jbmClusterNotification.sendMqttNotification(mqttNotification);
                break;
        }
        return true;
    }

//    public boolean exchange(PushMessage mqttNotification) {
//        Assert.notNull(realMqttPahoClientFactory, "MQTT链接未初始化");
//        MqttMessage message = new MqttMessage();
//        message.setPayload(JSON.toJSONBytes(mqttNotification));
//        String topic = "user/" + mqttNotification.getRecUserId();
//        if (ObjectUtil.isEmpty(mqttNotification.getRecUserId())) {
//            topic = "system/web";
//        }
//        try {
//            mqttClient.publish(topic, message);
//            if (StrUtil.isNotEmpty(mqttNotification.getTags())) {
//                List<String> tags = StrUtil.split(mqttNotification.getTags(), ",");
//                for (String tag : tags) {
//                    mqttClient.publish("tags/" + tag, message);
//                }
//            }
//            log.info("发送:{}成功", topic);
//        } catch (Exception e) {
//            log.error("发送站内信错误", e);
//            return false;
//        }
//        return true;
//    }
}
