package com.jbm.cluster.push.message;

import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.model.entitys.message.MqttNotification;
import com.jbm.cluster.push.handler.NotificationDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 消息生产者
 **/
@Slf4j
@Service
public class MqMessageProducer {
    //    @Autowired
//    @Output(MqMessageSource.MESSAGE_OUTPUT)
//    private MessageChannel channel;
//    @Autowired
//    @Output(QueueConstants.QUEUE_PUSH_MESSAGE + MqMessageSource.OUTPUT)
//    private MessageChannel notificationOutput;

    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @Scheduled(cron = "0/10 * *  * * ? ")
    public void sendTestClusterEvent() {
//        TestClusterEvent testClusterEvent = new TestClusterEvent();
//        testClusterEvent.setCreateTime(DateTime.now());
//        testClusterEvent.setTopic("test");
        MqttNotification mqttNotification = new MqttNotification();
        mqttNotification.setTopic("test");
        MessageHeaders messageHeaders = new MessageHeaders(Collections.singletonMap(MessageHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON));
        notificationDispatcher.sendNotification(mqttNotification);
//        notificationOutput.send(MessageBuilder.withPayload(testClusterEvent).build());
        log.info("【MQ发送内容】" + JSON.toJSONString(mqttNotification));
    }
}