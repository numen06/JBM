package com.jbm.cluster.node.client;

import com.jbm.cluster.api.model.message.Notification;
import com.jbm.cluster.common.constants.QueueConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-05 03:48
 **/
@Slf4j
public class NotificationClient {

    private final RabbitTemplate template;

    public NotificationClient(RabbitTemplate template) {
        this.template = template;
    }

    public void send(Notification notification) {
        template.convertAndSend(QueueConstants.QUEUE_PUSH_MESSAGE, notification);
    }
}
