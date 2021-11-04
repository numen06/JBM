package com.jbm.cluster.node;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import com.alibaba.fastjson.JSON;
import com.github.pfmiles.minvelocity.TemplateUtil;
import com.google.common.collect.Maps;
import com.jbm.cluster.api.model.ClusterEvent;
import com.jbm.cluster.api.model.entitys.message.MqttNotification;
import com.jbm.cluster.api.model.entitys.message.Notification;
import com.jbm.cluster.api.model.entitys.message.PushMessage;
import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.util.MapUtils;
import com.jbm.util.archive.Archive;
import com.jbm.util.archive.JarLoader;
import com.rabbitmq.client.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;

@Slf4j
@AllArgsConstructor
public class JbmClusterTemplate {

    //    @Autowired
    private RabbitTemplate rabbitTemplate;

    //    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


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
        log.info("集群节点启动成功[{}]:{},编译时间:{}", this.microServiceName, this.microServiceNode, "");
        this.printBanner(this.microServiceName);
    }

    public void printBanner(String serviceName) {
        try {
            Map<String, Object> data = Maps.newHashMap();
//            ExpressionParser parser = new SpelExpressionParser();
//            StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext(applicationContext);
            data.put("name", serviceName);
            data.put("build-time", "run in local");
            Archive archive = new JarLoader().findArchive();
            if (ObjectUtil.isNotEmpty(archive)) {
                Manifest manifest = archive.getManifest();
                if (ObjectUtil.isNotEmpty(manifest)) {
                    String buildTime = manifest.getMainAttributes().getValue("Build-Time");
                    if (StrUtil.isNotBlank(buildTime)) {
                        data.put("build-time", buildTime);
                    }
                }
            }
            String banner = TemplateUtil.render("jbm.banner", data);
//            InputStream inputStream = ResourceUtil.getStream("jbm.banner");
//            String txt = IoUtil.readUtf8(inputStream);
//            IoUtil.close(inputStream);
//            Expression expression = parser.parseExpression(txt);
//            String banner = expression.getValue(standardEvaluationContext, String.class);
            System.out.println(banner);
        } catch (Exception e) {
            log.error("打印节点信息错误", e);
        }
    }


    @EventListener(WebServerInitializedEvent.class)
    public void onWebServerInitializedEvent(WebServerInitializedEvent event) {
        microServicePort = event.getWebServer().getPort();
        this.loadNodeInfo();
    }


    private Map<String, CountDownLatch> clusterEventThreadMap = Maps.newConcurrentMap();

    private CountDownLatch getClusterEventThread(String key) {
        if (clusterEventThreadMap.containsKey(key))
            return clusterEventThreadMap.get(key);
        CountDownLatch countDownLatch = ThreadUtil.newCountDownLatch(1);
        clusterEventThreadMap.put(key, countDownLatch);
        return countDownLatch;
    }

    private void endClusterEventThread(String key) {
        MapUtil.removeAny(clusterEventThreadMap, key);
    }

    /**
     * 接收集群事件
     *
     * @param channel
     */
//    @Async
    @RabbitListener(queues = "#{pushEventQueue.name}")
    protected void handleClusterEvent(@Payload ClusterEvent clusterEvent, Channel channel, Message message) throws Exception {
        try {
//            clusterEvent = JSON.parseObject(message.getBody(), ClusterEvent.class);
            if (ObjectUtil.isEmpty(clusterEvent))
                return;
            clusterEvent.reviceBuild();
            log.info("接收到了集群事件:[{}],内容为:{}", clusterEvent.getClass().getName(), clusterEvent.toJson());
            //是否为强制消费队列
            applicationContext.publishEvent(clusterEvent);
            CountDownLatch countDownLatch = this.getClusterEventThread(clusterEvent.getEventId());
            //等待5分钟超时
            countDownLatch.await(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("接收到集群事件错误", e);
            throw e;
//            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        } finally {
            this.endClusterEventThread(clusterEvent.getEventId());
        }
//        log.info("推送本地任务完成");
    }

    /**
     * 释放集群事件
     *
     * @param clusterEvent
     */
    @EventListener
    public void localClusterEnvent(ClusterEvent clusterEvent) {
        if (BooleanUtil.isTrue(clusterEvent.getAck())) {
            log.info("本地加锁集群事件处理，开始释放事件:{}", clusterEvent.getEventId());
            CountDownLatch countDownLatch = this.getClusterEventThread(clusterEvent.getEventId());
//            Date startTime = DateTime.now();
            long countdownTime = 1;
            while (BooleanUtil.isTrue(clusterEvent.getAck())) {
                ThreadUtil.safeSleep(1000);
                countdownTime++;
                if (countdownTime % 30 == 0) {
                    log.info("活跃的本机集群事件为{}条,[{}]已处理{}秒", clusterEventThreadMap.size(), clusterEvent.getEventId(), countdownTime);
                }
            }
            countDownLatch.countDown();
            log.info("本地加锁集群事件处理，结束释放事件:{}", clusterEvent.getEventId());
        } else {
            CountDownLatch countDownLatch = this.getClusterEventThread(clusterEvent.getEventId());
            countDownLatch.countDown();
            log.info("本地非加锁集群事件处理完毕，释放事件:{}", clusterEvent.getEventId());
        }
    }


//    /**
//     * 释放集群事件
//     *
//     * @param clusterEvent
//     */
//    public void ackClusterEnvent(ClusterEvent clusterEvent) {
//        if (ObjectUtil.isEmpty(clusterEvent)) {
//            return;
//        }
//        String clazz = ClassUtil.getClassName(clusterEvent, false);
//        this.stringRedisTemplate.delete(clusterEvent.getEventId());
//    }


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
