package com.jbm.cluster.push.message;

import com.jbm.cluster.push.handler.NotificationDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

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
