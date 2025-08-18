package com.jbm.cluster.push.handler;

import cn.hutool.core.date.DateTime;
import com.jbm.cluster.api.entitys.message.Notification;
import com.jbm.cluster.core.constant.QueueConstants;
import com.jbm.cluster.push.usage.NotificationExchanger;
import com.jbm.util.batch.ActionBean;
import com.jbm.util.batch.RollingTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
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

//    private ExecutorService executorService;

    @Autowired
    private StreamBridge streamBridge;

    public NotificationDispatcher() {
//        Integer availableProcessors = Runtime.getRuntime().availableProcessors();
//        Integer numOfThreads = availableProcessors * 2;
//        executorService = new ThreadPoolExecutor(numOfThreads, numOfThreads, 0, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
//        log.info("Init Notification ExecutorService , numOfThread : " + numOfThreads);
    }

    public void sendNotification(Notification notification) {
        final Message<Notification> message = MessageBuilder.withPayload(notification)
                .setHeader("class", notification.getClass())
                .build();
        streamBridge.send(QueueConstants.NOTIFICATION_STREAM, message);
    }


    private final RollingTask<Long> countWithTime = RollingTask.createRollingTask(1L, TimeUnit.MINUTES, new Function<ActionBean<Long>, Long>() {
        @Override
        public Long apply(ActionBean<Long> actionBean) {
            log.info("消息队列最近1分钟处理日志:{}", actionBean.getCurrQuantity());
            return actionBean.getObj();
        }
    });

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
        if (notification != null && exchangers != null) {
            exchangers.parallelStream().forEach(exchanger -> {
                if (exchanger.support(notification)) {
                    exchanger.send(notification);
                }
            });
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, NotificationExchanger> beansOfType = applicationContext.getBeansOfType(NotificationExchanger.class);
        this.exchangers = beansOfType.values();
    }


}
