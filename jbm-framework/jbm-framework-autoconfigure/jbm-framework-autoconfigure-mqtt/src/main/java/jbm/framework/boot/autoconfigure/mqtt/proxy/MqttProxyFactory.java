package jbm.framework.boot.autoconfigure.mqtt.proxy;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.expression.engine.spel.SpELEngine;
import com.alibaba.fastjson.JSON;
import com.jbm.util.proxy.ReflectUtils;
import com.jbm.util.proxy.wapper.RequestHeaders;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttMapper;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttRequest;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttRequsetBean;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttResponseBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class MqttProxyFactory {

    private final ApplicationContext applicationContext;
    private final RealMqttPahoClientFactory mqttPahoClientFactory;

    public MqttProxyFactory(ApplicationContext applicationContext, RealMqttPahoClientFactory mqttPahoClientFactory) {
        this.applicationContext = applicationContext;
        this.mqttPahoClientFactory = mqttPahoClientFactory;
    }

//    private SimpleMqttClient simpleMqttClient;

    @PostConstruct
    public void find() throws MqttException {
        Map<String, Object> mqttProxys = applicationContext.getBeansWithAnnotation(MqttMapper.class);
        for (String name : mqttProxys.keySet()) {
            log.debug("class {} find mqtt proxy", name);
            Object bean = mqttProxys.get(name);
            MqttMapper mqttMapper = AnnotationUtil.getAnnotation(bean.getClass(), MqttMapper.class);
            String clientId = StrUtil.isBlank(mqttMapper.clientId()) ? MqttProxyFactory.class.getSimpleName() + IdUtil.fastSimpleUUID() : mqttMapper.clientId();
            SimpleMqttClient simpleMqttClient = mqttPahoClientFactory.getClientInstance(clientId);

            Field[] fields = ClassUtil.getDeclaredFields(bean.getClass());
            for (Field field : fields) {
                if (field.getType().equals(SimpleMqttClient.class))
                    ReflectUtil.setFieldValue(bean, field, simpleMqttClient);
            }

            List<Method> methods = ReflectUtils.findAnnotationMethods(bean.getClass(), MqttRequest.class);
            for (Method method : methods) {
                MqttRequsetBean mqttRequsetBean = new MqttRequsetBean();
                mqttRequsetBean.setMethod(method);
                mqttRequsetBean.setBean(bean);
                //如果方法上有注解说明需要监听来源
                MqttRequest mqttRequest = AnnotationUtil.getAnnotation(method, MqttRequest.class);
                mqttRequsetBean.setRequestTopic(this.buildTopic(bean, mqttMapper.value(), mqttRequest.fromTopic()));
                mqttRequsetBean.setResponseTopic(this.buildTopic(bean, mqttMapper.value(), mqttRequest.toTopic()));
                log.debug("mqtt request [{}]", mqttRequsetBean);
//                MqttResponse mqttResponse = AnnotationUtil.getAnnotation(method, MqttResponse.class);
//                if (mqttResponse != null)
//                    mqttRequsetBean.setResponseTopic(mqttResponse.topic());
                this.subscribeMethod(mqttRequsetBean, simpleMqttClient);
            }
        }
    }

    public String buildTopic(Object bean, String... url) {
        String str = StrUtil.concat(true, url);
        String[] vals = StrUtil.subBetweenAll(str, "${", "}");
        for (String val : vals) {
            String expected = StrUtil.concat(true, "#", val, "");
            String result = new SpELEngine().eval(expected, BeanUtil.beanToMap(bean), null).toString();
            str = StrUtil.replace(str, StrUtil.concat(true, "${", val, "}"), result);
        }
        return str;
    }

    private ThreadPoolExecutor threadPoolExecutor = ThreadUtil.newExecutor(5, 20);

    public void subscribeMethod(MqttRequsetBean mqttRequsetBean, SimpleMqttClient simpleMqttClient) throws MqttException {
        log.info("start subscribe mqtt topic to method:{}", mqttRequsetBean.getRequestTopic());
        simpleMqttClient.subscribeWithResponse(mqttRequsetBean.getRequestTopic(), new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
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
                });
            }
        });
    }


}
