package com.jbm.cluster.node.configuration;

import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.cluster.node.client.NotificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;


/**
 * @author wesley.zhang
 */
@Slf4j
//@Configuration
public class MqAutoConfiguration {

    /**
     * direct模式，直接根据队列名称投递消息
     *
     * @return
     */
    @Bean
    public Queue apiResourceQueue() {
        Queue queue = new Queue(QueueConstants.QUEUE_SCAN_API_RESOURCE);
        log.info("Query {} [{}]", QueueConstants.QUEUE_SCAN_API_RESOURCE, queue);
        return queue;
    }

    @Bean
    public Queue accessLogsQueue() {
        Queue queue = new Queue(QueueConstants.QUEUE_ACCESS_LOGS);
        log.info("Query {} [{}]", QueueConstants.QUEUE_ACCESS_LOGS, queue);
        return queue;
    }


    @Bean
    public Queue pushQueue() {
        Queue queue = new Queue(QueueConstants.QUEUE_PUSH_MESSAGE);
        log.info("Query {} [{}]", QueueConstants.QUEUE_PUSH_MESSAGE, queue);
        return queue;
    }


    @Bean
    public NotificationClient notificationClient(RabbitTemplate template) {
        return new NotificationClient(template);
    }

}
