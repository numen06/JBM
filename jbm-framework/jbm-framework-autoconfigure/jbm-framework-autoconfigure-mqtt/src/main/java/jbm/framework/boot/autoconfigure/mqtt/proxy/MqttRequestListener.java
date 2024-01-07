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

    public MqttRequestListener(MqttRequsetBean mqttRequsetBean, SimpleMqttClient simpleMqttClient) {
        this.mqttRequsetBean = mqttRequsetBean;
        this.simpleMqttClient = simpleMqttClient;
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
     * @param topic
     * @param message
     * @throws Exception
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                executeMqttRequest(topic, message);
            }
        });


    }
}