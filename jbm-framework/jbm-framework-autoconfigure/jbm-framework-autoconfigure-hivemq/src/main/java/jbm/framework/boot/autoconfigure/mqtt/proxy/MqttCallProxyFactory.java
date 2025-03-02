package jbm.framework.boot.autoconfigure.mqtt.proxy;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jbm.framework.boot.autoconfigure.mqtt.AbstractMqttMessageListener;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;
import jbm.framework.boot.autoconfigure.mqtt.proxy.call.MqttCallClientInterceptor;
import jbm.framework.boot.autoconfigure.mqtt.proxy.call.MqttCallContextHolder;
import jbm.framework.boot.autoconfigure.mqtt.proxy.call.MqttCallServiceInterceptor;
import jbm.framework.boot.autoconfigure.mqtt.proxy.call.MqttSubscribeProxy;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttCallBean;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttCallContext;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.function.Consumer;

/**
 * @author wesley
 */
@Slf4j
public class MqttCallProxyFactory {

    private final RealMqttPahoClientFactory mqttPahoClientFactory;

    private ByteBuddy byteBuddy = new ByteBuddy();

    public MqttCallProxyFactory(RealMqttPahoClientFactory mqttPahoClientFactory) {
        this.mqttPahoClientFactory = mqttPahoClientFactory;
    }

    public <T> T getClient(Class<T> clazz) {
        return this.registerClient(clazz);
    }

    private <T> T registerClient(Class<T> clazz) {
        try {
            MqttCallClient mqttCallClient = AnnotationUtil.getAnnotation(clazz, MqttCallClient.class);
            if (mqttCallClient == null) {
                throw new RuntimeException("未找到注解MqttCallClient");
            }
            SimpleMqttClient mqttClient = mqttPahoClientFactory.getClientInstance(mqttCallClient.clientId());
            MqttCallClientInterceptor mqttCallClientInterceptor = new MqttCallClientInterceptor(mqttClient, mqttCallClient);
            // 使用 ByteBuddy 创建代理类
            T proxy = byteBuddy
                    .subclass(clazz)
                    .method(ElementMatchers.isDeclaredBy(clazz))
                    .intercept(MethodDelegation.to(mqttCallClientInterceptor))
                    .make()
                    .load(clazz.getClassLoader())
                    .getLoaded()
                    .newInstance();
            MqttSubscribeProxy mqttSubscribeProxy = new MqttSubscribeProxy(true,mqttClient, clazz, mqttCallClient, proxy);
            return proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T getService(String name, Class<T> clazz) {
        //如果是接口则报错
        return this.registerService(name, clazz);
    }

    private <T> T registerService(String name, Class<T> clazz) {
        try {
            MqttCallClient mqttCallClient = AnnotationUtil.getAnnotation(clazz, MqttCallClient.class);
            if (mqttCallClient == null) {
                throw new RuntimeException("未找到注解MqttCallClient");
            }
            SimpleMqttClient mqttClient = mqttPahoClientFactory.getClientInstance(mqttCallClient.clientId());
            MqttCallServiceInterceptor mqttCallServerInterceptor = new MqttCallServiceInterceptor(mqttClient, mqttCallClient);
            T proxy = byteBuddy
                    .subclass(clazz)
                    .method(ElementMatchers.any())
                    .intercept(MethodDelegation.to(mqttCallServerInterceptor))
                    .make()
                    .load(clazz.getClassLoader())
                    .getLoaded()
                    .newInstance();
            // 创建代理实例
            MqttSubscribeProxy mqttSubscribeProxy = new MqttSubscribeProxy(mqttClient, clazz, mqttCallClient, proxy);
            return proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void requestAndResponseEvent(String requestTopic, String responseTopic, String eventCode, Object requestMessage, Consumer<MqttCallBean> consumer) {
        SimpleMqttClient simpleMqttClient = mqttPahoClientFactory.getClientInstance();
        subscribeMqttEvent(simpleMqttClient, responseTopic, eventCode, consumer);
        requestMqttEvent(simpleMqttClient, requestTopic, eventCode, requestMessage);
    }

    public void requestMqttEvent(String topic, String eventCode, Object message) {
        requestMqttEvent(mqttPahoClientFactory.getClientInstance(), topic, eventCode, message);
    }

    public static void requestMqttEvent(SimpleMqttClient simpleMqttClient, String requestTopic, String eventCode, Object message) {
        MqttCallContext mqttCallContext = new MqttCallContext(requestTopic, null, eventCode);
        mqttCallContext.putRequestMessage(message);
//        MqttCallContextHolder.set(mqttCallContext);
        // 发布消息到MQTT主题
        simpleMqttClient.publishObject(mqttCallContext.getRequestTopic(), mqttCallContext.getRequestBean());
    }

    public void subscribeMqttEvent(String responseTopic, String eventCode, Consumer<MqttCallBean> message) {
        subscribeMqttEvent(mqttPahoClientFactory.getClientInstance(), responseTopic, eventCode, message);
    }

    public static void subscribeMqttEvent(SimpleMqttClient simpleMqttClient, String responseTopic, String eventCode, Consumer<MqttCallBean> consumer) {
        // 发布消息到MQTT主题
        simpleMqttClient.subscribe(responseTopic, new AbstractMqttMessageListener() {

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                MqttCallBean mqttCallBean = JSON.parseObject(message.getPayload(), MqttCallBean.class);
                if (!mqttCallBean.getEventCode().equals(eventCode)) {
                    return;
                }
                consumer.accept(mqttCallBean);
            }
        });
    }


}
