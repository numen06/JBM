package com.jbm.cluster.node;

import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.model.ClusterEvent;
import com.jbm.cluster.api.model.entitys.message.MqttNotification;
import com.jbm.cluster.api.model.entitys.message.Notification;
import com.jbm.cluster.api.model.entitys.message.PushMessage;
import com.jbm.cluster.common.constants.QueueConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.handler.annotation.Payload;

@Slf4j
@AllArgsConstructor
public class JbmClusterTemplate {

    //    @Autowired
    private RabbitTemplate rabbitTemplate;

    //    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 接收集群事件
     *
     * @param clusterEvent
     */
    @RabbitListener(queues = "#{pushEventQueue.name}")
    public void handleClusterEvent(@Payload ClusterEvent clusterEvent) {
        clusterEvent.reviceBuild();
        log.info("接受到了集群事件,内容为:{}", clusterEvent.toJson());
        applicationContext.publishEvent(clusterEvent);
        log.info("推送本地任务完成");
    }

    /**
     * 发送集群事件
     *
     * @param clusterEvent
     */
    public void sendClusterEvent(ClusterEvent clusterEvent) {
        if (ObjectUtil.isEmpty(clusterEvent))
            throw new NullPointerException("集群事件为空");
        clusterEvent.sendBuild();
        rabbitTemplate.convertAndSend(QueueConstants.QUEUE_CLUSTER_EVENT_EXCHANGE, "", clusterEvent);
    }

    /**
     * 发送MQTT通知
     *
     * @param mqttNotification
     */
    public void sendMqttNotification(MqttNotification mqttNotification) {
        rabbitTemplate.convertAndSend(QueueConstants.QUEUE_PUSH_MESSAGE, mqttNotification);
    }

    /**
     * 发送集群通知
     *
     * @param notification
     */
    public void sendClusterNotification(Notification notification) {
        rabbitTemplate.convertAndSend(QueueConstants.QUEUE_PUSH_MESSAGE, notification);
    }


    /**
     * 发送站内信
     *
     * @param pushMessage
     */
    public void sendClusterNotification(PushMessage pushMessage) {
        rabbitTemplate.convertAndSend(QueueConstants.QUEUE_PUSH_MESSAGE, pushMessage);
    }

}
