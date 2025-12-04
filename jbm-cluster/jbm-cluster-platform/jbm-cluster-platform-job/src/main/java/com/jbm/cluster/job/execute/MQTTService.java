package com.jbm.cluster.job.execute;

import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author scolin
 * @description mqtt服务
 * @date 2025/10/22 11:47
 */
@Service
@Slf4j
public class MQTTService {
    @Resource
    private RealMqttPahoClientFactory deviceMqttPahoClientFactory;
    private SimpleMqttClient simpleMqttClient;

    @PostConstruct
    public void init() {
        try {
            this.simpleMqttClient = this.deviceMqttPahoClientFactory.getClientInstance();
            log.info("MQTT客户端初始化成功");
        } catch (Exception e) {
            log.error("流程引擎MQTT初始化失败，MQTT功能将不可用", e);
        }
    }
//    @Resource
//    private MqttPahoClientFactory mqttClientFactory;
//
//    @Resource
//    private ProcessTriggerService processTriggerService;
//
//    @Autowired
//    private ProcessExecutionEngine processExecutionEngine;
//
//    private MqttPahoClientHandler clientHandler;
//
//    @PostConstruct
//    public void init() {
//        connect();
//    }
//
//    public void connect() {
//        try {
//            clientHandler = mqttClientFactory.getClientHandler();
//            clientHandler.connect();
//
//            // 订阅所有等待中的触发器主题
//            subscribeToWaitingTriggers();
//
//        } catch (Exception e) {
//            log.error("MQTT连接失败", e);
//        }
//    }

    public void publish(String topic, String message) {
        if (simpleMqttClient == null) {
            log.error("MQTT客户端未初始化");
            return;
        }
        
        try {
            simpleMqttClient.publishObject(topic, message);
        } catch (Exception e) {
            log.error("流程引擎MQTT发布失败 - 主题: {}, 消息: {}", topic, message, e);
        }
    }
//
//    private void subscribeToWaitingTriggers() {
//        List<ProcessTrigger> waitingTriggers = processTriggerRepository.findByStatusAndTriggerType(ProcessStatusEnum.WAITING.getValue(), "MQTT");
//
//        for (ProcessTrigger trigger : waitingTriggers) {
//            subscribe(trigger.getTriggerKey(), trigger);
//        }
//    }
//
//    private void subscribe(String topic, ProcessTrigger trigger) {
//        try {
//            clientHandler.subscribe(topic, 1, (message) -> {
//                String payload = new String(message.getPayload());
//                handleMQTTMessage(trigger, payload);
//            });
//        } catch (Exception e) {
//            logger.error("MQTT订阅失败: " + topic, e);
//        }
//    }
//
//    private void handleMQTTMessage(ProcessTrigger trigger, String message) {
//        try {
//            // 继续执行流程
//            ExecuteProcessRequest request = new ExecuteProcessRequest();
//            request.setProcessInstanceId(trigger.getProcessInstanceId());
//            request.setTriggerNodeId(trigger.getNodeId());
//            request.setTriggerData(message);
//
//            processExecutionEngine.continueProcess(request);
//
//        } catch (Exception e) {
//            logger.error("处理MQTT消息失败", e);
//        }
//    }
}
