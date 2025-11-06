package jbm.framework.boot.autoconfigure.mqtt.proxy;

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

    public MqttRequestListener(MqttRequsetBean mqttRequsetBean, SimpleMqttClient simpleMqttClient) {
        this.mqttRequsetBean = mqttRequsetBean;
        this.simpleMqttClient = simpleMqttClient;
    }
    
    public MqttRequsetBean getMqttRequsetBean() {
        return mqttRequsetBean;
    }

    private final ExecutorService executor = Executors.newCachedThreadPool();

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
     * 消息到达处理
     * 注意：由于已经通过多播监听器机制处理了单个类方法只执行一次的问题，这里不再需要消息级别的去重
     * 
     * @param topic
     * @param message
     * @throws Exception
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        log.debug("📨 收到消息 Topic: {}, Method: {}", 
                topic, 
                mqttRequsetBean.getMethod().getName());
        
        // 提交到线程池异步处理
        executor.submit(() -> {
            try {
                executeMqttRequest(topic, message);
                log.debug("✅ 消息处理完成 Topic: {}, Method: {}", 
                        topic, mqttRequsetBean.getMethod().getName());
            } catch (Exception e) {
                log.error("❌ 消息处理失败 Topic: {}, Method: {}", 
                        topic, mqttRequsetBean.getMethod().getName(), e);
            }
        });
    }
}