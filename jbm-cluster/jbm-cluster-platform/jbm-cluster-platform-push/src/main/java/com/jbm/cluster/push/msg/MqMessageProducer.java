package com.jbm.cluster.push.msg;

import cn.hutool.core.date.DateTime;
import com.jbm.cluster.api.TestClusterEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 消息生产者
 **/
@EnableBinding({MqMessageSource.class})
@Slf4j
public class MqMessageProducer {
    @Autowired
    @Output(MqMessageSource.MESSAGE_OUTPUT)
    private MessageChannel channel;


//    @Scheduled(cron = "0/10 * *  * * ? ")
    public void sendTestClusterEvent() {
        TestClusterEvent testClusterEvent = new TestClusterEvent();
        testClusterEvent.setCreateTime(DateTime.now());
        testClusterEvent.setTopic("test");
        channel.send(MessageBuilder.withPayload(testClusterEvent).build());
        log.info("【MQ发送内容】" + JSON.toJSONString(testClusterEvent));
    }
}