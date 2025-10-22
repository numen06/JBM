package jbm.framework.boot.autoconfigure.mqtt.proxy;

import cn.hutool.core.thread.AsyncUtil;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.util.proxy.ReflectUtils;
import com.jbm.util.proxy.wapper.RequestHeaders;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttRequsetBean;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttResponseBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.*;

/**
 * @author wesley
 */
@Slf4j
public class MqttRequestListener implements IMqttMessageListener {


    private final MqttRequsetBean mqttRequsetBean;
    private final SimpleMqttClient simpleMqttClient;
    
    /**
     * 用于跟踪正在处理的消息，防止重复处理
     * key: messageId (由消息ID和topic组成)
     * value: 处理时间戳
     */
    private final ConcurrentHashMap<String, Long> processingMessages = new ConcurrentHashMap<>();
    
    /**
     * 消息去重的超时时间（毫秒），默认30秒
     * 超过这个时间的消息处理记录会被清理
     */
    private static final long MESSAGE_TIMEOUT = 30000L;

    public MqttRequestListener(MqttRequsetBean mqttRequsetBean, SimpleMqttClient simpleMqttClient) {
        this.mqttRequsetBean = mqttRequsetBean;
        this.simpleMqttClient = simpleMqttClient;
        // 启动清理任务
        startCleanupTask();
    }

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * 启动定期清理过期消息记录的任务
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                processingMessages.entrySet().removeIf(entry -> 
                    currentTime - entry.getValue() > MESSAGE_TIMEOUT
                );
            } catch (Exception e) {
                log.error("清理过期消息记录失败", e);
            }
        }, MESSAGE_TIMEOUT, MESSAGE_TIMEOUT, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 生成消息唯一标识
     */
    private String generateMessageKey(String topic, MqttMessage message) {
        return topic + "_" + message.getId() + "_" + message.hashCode();
    }

    public void executeMqttRequest(String topic, MqttMessage message) {
        try {
            RequestHeaders requestHeader = new RequestHeaders();
            requestHeader.set("topic", topic);
            Object result = ReflectUtils.invokeMethodFromJsonData(mqttRequsetBean.getBean(), mqttRequsetBean.getMethod(), StrUtil.str(message.getPayload(), Charsets.UTF_8), requestHeader);
            if (ObjectUtil.isNotEmpty(result) && result instanceof MqttResponseBean) {
                MqttResponseBean mqttResponseBean = (MqttResponseBean) result;
                MqttMessage mqttMessage = new MqttMessage();
                if (ObjectUtil.isNotEmpty(mqttResponseBean.getBody()) && mqttResponseBean.getBody() instanceof String) {
                    mqttMessage.setPayload(StrUtil.bytes(mqttResponseBean.getBody().toString()));
                } else {
                    mqttMessage.setPayload(JSON.toJSONBytes(mqttResponseBean.getBody()));
                }
                mqttMessage.setQos(mqttResponseBean.getQos());
                simpleMqttClient.publish(mqttResponseBean.getTopic(), mqttMessage);
            } else if (ObjectUtil.isAllNotEmpty(mqttRequsetBean.getResponseTopic())) {
                if (String.class.equals(mqttRequsetBean.getMethod().getReturnType())) {
                    MqttMessage mqttMessage = new MqttMessage();
                    mqttMessage.setPayload(JSON.toJSONBytes(StrUtil.bytes(StrUtil.toString(result))));
                    simpleMqttClient.publish(mqttRequsetBean.getResponseTopic(), mqttMessage);
                } else {
                    simpleMqttClient.publishObject(mqttRequsetBean.getResponseTopic(), result);
                }
            }
        } catch (Exception e) {
            log.error("执行MQTT代理方法失败", e);
        }
    }

    /**
     * @param topic
     * @param message
     * @throws Exception
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // 生成消息唯一标识
        String messageKey = generateMessageKey(topic, message);
        
        // 尝试将消息标记为正在处理，如果已存在则说明正在处理中，直接返回
        Long existingTimestamp = processingMessages.putIfAbsent(messageKey, System.currentTimeMillis());
        if (existingTimestamp != null) {
            // 消息正在处理中，记录日志并跳过
            log.warn("消息[{}]正在处理中，跳过重复处理。Topic: {}", messageKey, topic);
            return;
        }
        
        // 提交到线程池异步处理
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    executeMqttRequest(topic, message);
                } finally {
                    // 处理完成后，延迟移除消息标记（防止短时间内的重复消息）
                    cleanupExecutor.schedule(() -> {
                        processingMessages.remove(messageKey);
                        log.debug("消息[{}]处理完成，已清理标记", messageKey);
                    }, 5, TimeUnit.SECONDS);
                }
            }
        });
    }
}