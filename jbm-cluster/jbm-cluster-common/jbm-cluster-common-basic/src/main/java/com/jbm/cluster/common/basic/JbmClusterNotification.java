package com.jbm.cluster.common.basic;

import com.jbm.cluster.api.entitys.message.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

@Slf4j
@AllArgsConstructor
public class JbmClusterNotification {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private StreamBridge streamBridge;

    public JbmClusterNotification() {
    }

    public void sendNotification(Notification notification) {
        final Message<Notification> message = MessageBuilder.withPayload(notification)
                .build();
        streamBridge.send("notification-in-0", message);
    }

//    /**
//     * 释放集群事件
//     *
//     * @param clusterEvent
//     */
//    public void ackClusterEnvent(ClusterEvent clusterEvent) {
//        if (ObjectUtil.isEmpty(clusterEvent)) {
//            return;
//        }
//        String clazz = ClassUtil.getClassName(clusterEvent, false);
//        this.stringRedisTemplate.delete(clusterEvent.getEventId());
//    }

    /**
     * 发送MQTT通知（站内信）
     *
     * @param mqttNotification
     */
    public void sendMqttNotification(MqttNotification mqttNotification) {
        this.sendNotification(mqttNotification);
    }

    /**
     * 发送邮件通知
     *
     * @param emailNotification
     */
    public void sendEmailNotification(EmailNotification emailNotification) {
        this.sendNotification(emailNotification);
    }

    /**
     * 发送短消息通知
     *
     * @param smsNotification
     */
    public void sendSmsNotification(SmsNotification smsNotification) {
        this.sendNotification(smsNotification);
    }

    /**
     * 发送站内信
     *
     * @param pushMessage
     */
    public void sendPushMessage(PushMessage pushMessage) {
        rabbitTemplate.convertAndSend(QueueConstants.QUEUE_PUSH_MESSAGE, pushMessage);
    }

}
