package com.jbm.cluster.push.handler;

import com.jbm.cluster.api.entitys.message.MqttNotification;
import com.jbm.cluster.api.entitys.message.Notification;
import com.jbm.cluster.api.model.push.PushCallback;
import com.jbm.cluster.api.model.push.PushMsg;
import com.jbm.cluster.push.service.PushMessageBodyService;
import com.jbm.cluster.push.service.PushMessageItemService;
import com.jbm.cluster.push.usage.MqttNotificationExchanger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Function;

/**
 * 通知处理器配置类
 */
@Slf4j
@Configuration
public class NotificationHandler {


    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @Autowired(required = false)
    private MqttNotificationExchanger mqttNotificationExchanger;

    @Autowired
    private PushMessageItemService pushMessageItemService;

    @Bean
    public Function<Flux<Message<Notification>>, Mono<Void>> notification() {
        return flux -> flux.flatMap(message ->
                Mono.fromRunnable(() -> notificationDispatcher.receive(message))
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(e -> {
                            log.error("notification 通道消费失败", e);
                            return Mono.empty();
                        })
                        .then()
        ).then();
    }

    /**
     * 接受MQTT的消息
     *
     * @return
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(MqttNotificationExchanger.class)
    public Function<Flux<Message<MqttNotification>>, Mono<Void>> mqtt() {
        return flux -> flux.flatMap(message ->
                Mono.fromRunnable(() -> mqttNotificationExchanger.send(message.getPayload()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(e -> {
                            log.error("mqtt 通道消费失败", e);
                            return Mono.empty();
                        })
                        .then()
        ).then();
    }

    /**
     * 接受MQTT的消息
     *
     * @return
     */
    @Bean
    public Function<Flux<Message<PushCallback>>, Mono<Void>> pushCallBack() {
        return flux -> flux.flatMap(message ->
                Mono.fromRunnable(() -> pushMessageItemService.sendCallBack(message.getPayload()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(e -> {
                            log.error("pushCallBack 通道消费失败", e);
                            return Mono.empty();
                        })
                        .then()
        ).then();
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
