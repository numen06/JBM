package com.jbm.cluster.push.handler;

import com.jbm.cluster.api.entitys.message.Notification;
import com.jbm.cluster.core.constant.QueueConstants;
import com.jbm.cluster.push.usage.NotificationExchanger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author wesley.zhang
 * @date 2018-3-27
 **/
@Component
@Slf4j
public class NotificationDispatcher implements ApplicationContextAware {


    private Collection<NotificationExchanger> exchangers;

    private ExecutorService executorService;

    @Autowired
    private StreamBridge streamBridge;

    public NotificationDispatcher() {
        Integer availableProcessors = Runtime.getRuntime().availableProcessors();
        Integer numOfThreads = availableProcessors * 2;
        executorService = new ThreadPoolExecutor(numOfThreads, numOfThreads, 0, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        log.info("Init Notification ExecutorService , numOfThread : " + numOfThreads);
    }

    public void sendNotification(Notification notification) {
        final Message<Notification> message = MessageBuilder.withPayload(notification)
                .setHeader("class", notification.getClass())
                .build();
        streamBridge.send(QueueConstants.NOTIFICATION_STREAM, message);
    }

    @Bean
    public Function<Flux<Message<Notification>>, Mono<Void>> notification() {
        return flux -> flux.map(message -> {
            this.receive(message);
            return message;
        }).then();
    }

    //    @StreamListener(QueueConstants.QUEUE_PUSH_MESSAGE + MqMessageSource.INPUT)
    public void receive(Message<Notification> message) {
        Notification notification = message.getPayload();
        if (notification != null && exchangers != null) {
            exchangers.forEach((exchanger) -> {
                if (exchanger.support(notification)) {
                    //添加到线程池进行处理
                    executorService.submit(new NotificationTask(exchanger, notification));
                }
            });
        }
    }

    public void dispatch(Notification notification) {
        if (notification != null && exchangers != null) {
            exchangers.forEach((exchanger) -> {
                if (exchanger.support(notification)) {
                    //添加到线程池进行处理
                    executorService.submit(new NotificationTask(exchanger, notification));
                }
            });
        }
    }

//    @RabbitHandler
//    public void dispatch(@Payload Notification notification) {
//        if (notification != null && exchangers != null) {
//            exchangers.forEach((exchanger) -> {
//                if (exchanger.support(notification)) {
//                    //添加到线程池进行处理
//                    executorService.submit(new NotificationTask(exchanger, notification));
//                }
//            });
//        }
//    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, NotificationExchanger> beansOfType = applicationContext.getBeansOfType(NotificationExchanger.class);
        this.exchangers = beansOfType.values();
    }


}
