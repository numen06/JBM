package com.jbm.cluster.node.configuration;

import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.cluster.node.JbmClusterTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;


/**
 * @author wesley.zhang
 */
@Slf4j
//@Configuration
public class MqAutoConfiguration {

    @Value("${spring.application.name:}")
    private String appTitle;

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
    public Queue dicResourceQueue() {
        Queue queue = new Queue(QueueConstants.QUEUE_SCAN_DIC_RESOURCE);
        log.info("Query {} [{}]", QueueConstants.QUEUE_SCAN_DIC_RESOURCE, queue);
        return queue;
    }

    @Bean
    public Queue pushEventQueue() {
        Queue queue = new Queue(QueueConstants.QUEUE_CLUSTER_EVENT + "." + appTitle);
        log.info("Query {} [{}]", queue.getName(), queue);
        return queue;
    }

    //创建Fanout交换器
    @Bean
    FanoutExchange fanoutExchange() {
        return new FanoutExchange(QueueConstants.QUEUE_CLUSTER_EVENT_EXCHANGE);
    }

    @Bean
    Binding bindingExchange(Queue pushEventQueue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(pushEventQueue).to(fanoutExchange);
    }

//    @Bean
//    public NotificationClient notificationClient(RabbitTemplate template) {
//        return new NotificationClient(template);
//    }

    @Bean
    public JbmClusterTemplate jbmClusterTemplate(RabbitTemplate rabbitTemplate,ApplicationContext applicationContext) {
        return new JbmClusterTemplate(rabbitTemplate,applicationContext);
    }





}
