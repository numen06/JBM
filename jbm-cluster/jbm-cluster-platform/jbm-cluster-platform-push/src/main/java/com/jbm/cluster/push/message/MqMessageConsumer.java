package com.jbm.cluster.push.message;

import com.jbm.cluster.api.model.entitys.message.Notification;
import com.jbm.cluster.push.handler.NotificationDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Configuration
public class MqMessageConsumer {

    @Autowired
    private NotificationDispatcher notificationDispatcher;

//    @Bean
//    public Function<Flux<Message<Notification>>, Mono<Void>> notification() {
//        return flux -> flux.map(message -> {
////            System.out.println(message.getPayload());
//            notificationDispatcher.receive(message);
//            return message;
//        }).then();
//    }




}
