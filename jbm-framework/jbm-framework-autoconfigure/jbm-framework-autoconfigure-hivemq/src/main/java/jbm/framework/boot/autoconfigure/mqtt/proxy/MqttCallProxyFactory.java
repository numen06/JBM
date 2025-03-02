package jbm.framework.boot.autoconfigure.mqtt.proxy;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReflectUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jbm.util.proxy.ReflectUtils;
import jbm.framework.boot.autoconfigure.mqtt.AbstractMqttMessageListener;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallEvent;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttRequest;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;
import jbm.framework.boot.autoconfigure.mqtt.proxy.call.MqttCallClientInterceptor;
import jbm.framework.boot.autoconfigure.mqtt.proxy.call.MqttCallServiceInterceptor;
import jbm.framework.boot.autoconfigure.mqtt.proxy.call.MqttSubscribeProxy;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttCallEventBean;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author wesley
 */
@Slf4j
public class MqttCallProxyFactory {

    private final Cache<String, Object> MQTTCALL_SERVICE_CACHE = Caffeine.newBuilder().build();
    private final Cache<String, Object> MQTTCALL_CLIENT_CACHE = Caffeine.newBuilder().build();

    private final RealMqttPahoClientFactory mqttPahoClientFactory;

    public MqttCallProxyFactory(RealMqttPahoClientFactory mqttPahoClientFactory) {
        this.mqttPahoClientFactory = mqttPahoClientFactory;
    }

    public <T>T getClient(Class<T> clazz) {
        if (MQTTCALL_CLIENT_CACHE.getIfPresent(clazz) != null) {
            return (T) MQTTCALL_CLIENT_CACHE.getIfPresent(clazz);
        }
        return this.registerClient(clazz);
    }

    private <T>T registerClient(Class<T> clazz) {
        try {
            MqttCallClient mqttCallClient = AnnotationUtil.getAnnotation(clazz, MqttCallClient.class);
            if (mqttCallClient == null) {
                throw new RuntimeException("未找到注解MqttCallClient");
            }
            SimpleMqttClient mqttClient = mqttPahoClientFactory.getClientInstance(mqttCallClient.clientId());
            MqttCallClientInterceptor mqttCallClientInterceptor = new MqttCallClientInterceptor(mqttClient, mqttCallClient);
            // 使用 ByteBuddy 创建代理类
            Class<?> proxyClass = new ByteBuddy()
                    .subclass(clazz)
                    .method(ElementMatchers.any())
                    .intercept(MethodDelegation.to(mqttCallClientInterceptor))
                    .make()
                    .load(clazz.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded();
            // 创建代理实例
            T proxy = (T) proxyClass.getDeclaredConstructor().newInstance();
            MqttSubscribeProxy mqttSubscribeProxy = new MqttSubscribeProxy(mqttClient, clazz, mqttCallClient, proxy);
            MQTTCALL_CLIENT_CACHE.put(clazz.getName(), proxy);
            return proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T getService(String name,  Class<T> clazz) {
        //如果是接口则报错
        T service = (T) MQTTCALL_SERVICE_CACHE.getIfPresent(name);
        if (service != null) {
            return service;
        }
        service = this.registerService(name, clazz);
        return service;
    }

    private <T> T registerService(String name, Class<T> clazz) {
        try {
            MqttCallClient mqttCallClient = AnnotationUtil.getAnnotation(clazz, MqttCallClient.class);
            if (mqttCallClient == null) {
                throw new RuntimeException("未找到注解MqttCallClient");
            }
            SimpleMqttClient mqttClient = mqttPahoClientFactory.getClientInstance(mqttCallClient.clientId());
            MqttCallServiceInterceptor mqttCallServerInterceptor = new MqttCallServiceInterceptor(mqttClient, mqttCallClient);
            Class<?> dynamicType = new ByteBuddy()
                    .subclass(clazz)
                    .method(ElementMatchers.any())
                    .intercept(MethodDelegation.to(mqttCallServerInterceptor))
                    .make()
                    .load(clazz.getClassLoader())
                    .getLoaded();
            // 创建代理实例
            T proxy = (T) dynamicType.getDeclaredConstructor().newInstance();
            MqttSubscribeProxy mqttSubscribeProxy = new MqttSubscribeProxy(mqttClient, clazz, mqttCallClient, proxy);
            MQTTCALL_SERVICE_CACHE.put(name, proxy);
            return proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void requestMqttEvent(String topic, String eventCode, Object message) {
        MqttCallEventBean mqttCallEventBean = new MqttCallEventBean();
        mqttCallEventBean.setEventId(IdUtil.simpleUUID());
        mqttCallEventBean.setMessage(message);
        mqttCallEventBean.setTopic(topic);
        mqttCallEventBean.setEventCode(eventCode);
        // 发布消息到MQTT主题
        mqttPahoClientFactory.getClientInstance().publishObject(mqttCallEventBean.getTopic(),mqttCallEventBean);
    }



//    private void fromBean(Object bean) {
//        MqttCallClient mqttCallClient = AnnotationUtil.getAnnotation(bean.getClass(), MqttCallClient.class);
//        SimpleMqttClient simpleMqttClient = mqttPahoClientFactory.getClientInstance(mqttCallClient.clientId());
//    }




}
