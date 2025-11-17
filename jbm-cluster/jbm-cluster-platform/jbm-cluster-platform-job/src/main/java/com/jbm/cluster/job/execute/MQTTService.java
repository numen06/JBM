package com.jbm.cluster.job.execute;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author scolin
 * @description mqtt服务
 * @date 2025/10/22 11:47
 */
@Component
@Slf4j
public class MQTTService {
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
//
//    public void publish(String topic, String message) {
//        try {
//            clientHandler.publish(topic, message.getBytes(), 1, false);
//        } catch (Exception e) {
//            logger.error("MQTT发布失败", e);
//        }
//    }
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
