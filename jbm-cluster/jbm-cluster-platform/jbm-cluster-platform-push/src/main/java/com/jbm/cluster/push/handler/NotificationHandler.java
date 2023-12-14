package com.jbm.cluster.push.handler;

import com.jbm.cluster.api.entitys.message.MqttNotification;
import com.jbm.cluster.api.entitys.message.Notification;
import com.jbm.cluster.api.model.push.PushCallback;
import com.jbm.cluster.api.model.push.PushMsg;
import com.jbm.cluster.push.service.PushMessageBodyService;
import com.jbm.cluster.push.service.PushMessageItemService;
import com.jbm.cluster.push.usage.MqttNotificationExchanger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 通知处理器配置类
 */
@Configuration
public class NotificationHandler {


    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @Autowired
    private MqttNotificationExchanger mqttNotificationExchanger;

    @Autowired
    private PushMessageItemService pushMessageItemService;

    @Bean
    public Function<Flux<Message<Notification>>, Mono<Void>> notification() {
        return flux -> flux.map(message -> {
            notificationDispatcher.receive(message);
            return message;
        }).then();
    }

    /**
     * 接受MQTT的消息
     *
     * @return
     */
    @Bean
    public Function<Flux<Message<MqttNotification>>, Mono<Void>> mqtt() {
        return flux -> flux.map(message -> {
            mqttNotificationExchanger.send(message.getPayload());
            return message;
        }).then();
    }

    /**
     * 接受MQTT的消息
     *
     * @return
     */
    @Bean
    public Function<Flux<Message<PushCallback>>, Mono<Void>> pushCallBack() {
        return flux -> flux.map(message -> {
            pushMessageItemService.sendCallBack(message.getPayload());
            return message;
        }).then();
    }

    @Autowired
    private PushMessageBodyService pushMessageBodyService;

    @Bean
    public Function<Flux<Message<PushMsg>>, Mono<Void>> pushMsg() {
        return flux -> flux.map(message -> {
            pushMessageBodyService.sendPushMsg(message.getPayload());
            return message;
        }).then();
    }
}
