package com.jbm.cluster.push.msg;

import com.alibaba.fastjson.JSONObject;
import com.jbm.cluster.api.TestClusterEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

@Slf4j
@EnableBinding(MqMessageSource.class)
public class MqMessageConsumer {


    /**
     * 消费ECM的货柜模板变更
     *
     * @param message
     */
    @StreamListener(MqMessageSource.Consumer)
    public void receive(Message<TestClusterEvent> message) {
        log.info("接收货柜模板开始，参数={}", JSONObject.toJSONString(message));
        if (null == message) {
            return;
        }
        try {
            TestClusterEvent jsonObject = message.getPayload();
            log.info("【MQ消费货柜模板更新信息成功】:{}", jsonObject);
        } catch (Exception e) {
            log.error("接收处理货柜模板MQ时出现异常:{}", e);
            throw new RuntimeException(e);
        }
    }
}
