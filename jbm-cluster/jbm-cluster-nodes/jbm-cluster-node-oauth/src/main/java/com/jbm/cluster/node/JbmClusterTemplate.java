package com.jbm.cluster.node;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pfmiles.minvelocity.TemplateUtil;
import com.google.common.collect.Maps;
import com.jbm.cluster.api.entitys.message.MqttNotification;
import com.jbm.cluster.api.entitys.message.Notification;
import com.jbm.cluster.api.entitys.message.PushMessage;
import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.util.archive.Archive;
import com.jbm.util.archive.JarLoader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
