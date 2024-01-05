package com.jbm.cluster.common.basic.module;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.message.EmailNotification;
import com.jbm.cluster.api.entitys.message.MqttNotification;
import com.jbm.cluster.api.entitys.message.Notification;
import com.jbm.cluster.api.entitys.message.SmsNotification;
import com.jbm.cluster.api.model.push.PushMsg;
import com.jbm.cluster.core.constant.QueueConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class JbmClusterNotification {

//    @Autowired
//    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private StreamBridge streamBridge;

    public JbmClusterNotification() {
    }


    public void sendNotification(Notification notification) {
        try {
            final Message<Notification> message = MessageBuilder.withPayload(notification)
                    .build();
            streamBridge.send(QueueConstants.NOTIFICATION_STREAM, message);
        } catch (Exception e) {
            log.error("发送消息错误", e);
        }
    }


    public void pushMsg(PushMsg pushMsg) {
        final Message<PushMsg> message = MessageBuilder.withPayload(pushMsg)
                .build();
        streamBridge.send(QueueConstants.PUSH_MESSAGE_STREAM, message);
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


}
