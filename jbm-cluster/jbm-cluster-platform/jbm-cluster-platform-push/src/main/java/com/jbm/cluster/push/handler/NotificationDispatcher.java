package com.jbm.cluster.push.handler;

import cn.hutool.core.date.DateTime;
import com.jbm.cluster.api.entitys.message.Notification;
import com.jbm.cluster.core.constant.QueueConstants;
import com.jbm.cluster.push.configuration.AsyncConfig;
import com.jbm.cluster.push.usage.NotificationExchanger;
import com.jbm.util.batch.ActionBean;
import com.jbm.util.batch.RollingTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
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

    @Autowired
    @Qualifier(AsyncConfig.NOTIFICATION_DISPATCHER_EXECUTOR)
    private ExecutorService notificationDispatcherExecutor;

    @Autowired
    private StreamBridge streamBridge;

    public void sendNotification(Notification notification) {
        final Message<Notification> message = MessageBuilder.withPayload(notification)
                .setHeader("class", notification.getClass())
                .build();
        streamBridge.send(QueueConstants.NOTIFICATION_STREAM, message);
    }


    private final RollingTask<Long> countWithTime = RollingTask.createRollingTask(1L, TimeUnit.MINUTES, new Function<ActionBean<Long>, Long>() {
        @Override
        public Long apply(ActionBean<Long> actionBean) {
            log.info("消息队列最近1分钟处理消息:{}", actionBean.getCurrQuantity());
            return actionBean.getObj();
        }
    });

    private final Map<String, RollingTask<Long>> channelCountWithTime = new java.util.concurrent.ConcurrentHashMap<>();

    public void receive(Message<Notification> message) {
        Notification notification = message.getPayload();
        try {
            notification.setSendTime(message.getHeaders().get("amqp_timestamp", Date.class));
        } catch (Exception e) {
            notification.setSendTime(DateTime.now());
        }
        this.dispatch(notification);
        countWithTime.offer();
    }

    public void dispatch(Notification notification) {
        if (notification == null || exchangers == null) {
            return;
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (NotificationExchanger exchanger : exchangers) {
            if (exchanger.support(notification)) {
                String channelName = exchanger.getClass().getSimpleName();
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        exchanger.send(notification);
                    } catch (Exception e) {
                        log.error("通知分发失败, 通道[{}]: {}", channelName, e.getMessage(), e);
                    }
                }, notificationDispatcherExecutor));
                channelCountWithTime.computeIfAbsent(channelName, key ->
                        RollingTask.createRollingTask(1L, TimeUnit.MINUTES, actionBean -> {
                            log.info("通道[{}]最近1分钟处理消息:{}", key, actionBean.getCurrQuantity());
                            return actionBean.getObj();
                        })
                ).offer();
            }
        }
        if (futures.isEmpty()) {
            return;
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("通知分发等待完成异常", e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, NotificationExchanger> beansOfType = applicationContext.getBeansOfType(NotificationExchanger.class);
        this.exchangers = beansOfType.values();
    }


}
