package com.jbm.cluster.node;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.model.ClusterEvent;
import com.jbm.cluster.api.model.entitys.message.MqttNotification;
import com.jbm.cluster.api.model.entitys.message.Notification;
import com.jbm.cluster.api.model.entitys.message.PushMessage;
import com.jbm.cluster.common.constants.QueueConstants;
import com.rabbitmq.client.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.Map;

@Slf4j
@AllArgsConstructor
public class JbmClusterTemplate {

    //    @Autowired
    private RabbitTemplate rabbitTemplate;

    //    @Autowired
    private ApplicationContext applicationContext;


    public JbmClusterTemplate(RabbitTemplate rabbitTemplate, ApplicationContext applicationContext) {
        this.rabbitTemplate = rabbitTemplate;
        this.applicationContext = applicationContext;
    }


    @Value("${spring.application.name:}")
    private String microServiceName;

    private Integer microServicePort;

    private String microServiceNode;

    private void loadNodeInfo() {
        String ip = NetUtil.getLocalhostStr();
        this.microServiceNode = String.format("%s:%s", ip, this.microServicePort);
        log.info("集群节点启动成功[{}]:{}", this.microServiceName, this.microServiceNode);
    }


    @EventListener(WebServerInitializedEvent.class)
    public void onWebServerInitializedEvent(WebServerInitializedEvent event) {
        microServicePort = event.getWebServer().getPort();
        this.loadNodeInfo();
    }

    /**
     * 接收集群事件
     *
     * @param bytes
     * @param channel
     */
    @RabbitListener(queues = "#{pushEventQueue.name}")
    public void handleClusterEvent(@Payload ClusterEvent clusterEvent, Channel channel, @Headers Map<String, Object> headers) {
        try {
//            clusterEvent = JSON.parseObject(message.getBody(), ClusterEvent.class);
            clusterEvent.reviceBuild();
        } catch (Exception e) {

        }
        if (ObjectUtil.isEmpty(clusterEvent))
            return;
        log.info("接受到了集群事件:[{}],内容为:{}", clusterEvent.getClass().getName(), clusterEvent.toJson());
        applicationContext.publishEvent(clusterEvent);
//        log.info("推送本地任务完成");
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
        log.info("客户端发送集群事件:{}", JSON.toJSONString(clusterEvent));
        rabbitTemplate.convertAndSend(QueueConstants.QUEUE_CLUSTER_EVENT_EXCHANGE, "", clusterEvent);
    }

    /**
     * 发送MQTT通知
     *
     * @param mqttNotification
     */
    public void sendMqttNotification(MqttNotification mqttNotification) {
        mqttNotification.sendBuild(this.microServiceName);
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
    public void sendPushMessage(PushMessage pushMessage) {
        rabbitTemplate.convertAndSend(QueueConstants.QUEUE_PUSH_MESSAGE, pushMessage);
    }

}
