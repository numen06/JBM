package com.jbm.cluster.job.execute;

import cn.hutool.json.JSONUtil;
import com.jbm.cluster.api.model.job.rule.NodeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author scolin
 * @description MQTT节点执行器
 * @date 2025/10/22 11:46
 */
@Component
public class MQTTNodeExecutor implements NodeExecutor{
    @Autowired
    private MQTTService mqttService;

    @Override
    public NodeExecutionResult execute(NodeData node, Map<String, Object> inputData) {
        try {
            Map<String, Object> nodeData = node.getData();
            String topic = (String) nodeData.get("topic");
            String action = (String) nodeData.get("action"); // PUBLISH or SUBSCRIBE

            if ("PUBLISH".equals(action)) {
                // 发布消息
                String message = buildMessage(nodeData, inputData);
                //mqttService.publish(topic, message);

                Map<String, Object> outputData = new HashMap<>(inputData);
                outputData.put("mqttPublished", true);
                outputData.put("mqttTopic", topic);
                outputData.put("mqttMessage", message);

                return NodeExecutionResult.success(outputData);

            } else if ("SUBSCRIBE".equals(action)) {
                // 订阅主题并等待消息
                return NodeExecutionResult.waiting("MQTT", topic);
            } else {
                return NodeExecutionResult.error("不支持的MQTT操作: " + action);
            }

        } catch (Exception e) {
            return NodeExecutionResult.error("MQTT操作失败: " + e.getMessage());
        }
    }

    private String buildMessage(Map<String, Object> nodeData, Map<String, Object> inputData) {
        String messageTemplate = (String) nodeData.get("message");
        if (messageTemplate == null) {
            return JSONUtil.toJsonStr(inputData);
        }

        // 模板变量替换
        for (Map.Entry<String, Object> entry : inputData.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            messageTemplate = messageTemplate.replace(placeholder,
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }

        return messageTemplate;
    }

    @Override
    public String getSupportedType() {
        return "mqtt";
    }
}
