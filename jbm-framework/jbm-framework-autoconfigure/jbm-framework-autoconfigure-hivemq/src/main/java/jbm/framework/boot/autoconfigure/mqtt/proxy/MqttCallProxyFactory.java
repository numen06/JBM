package jbm.framework.boot.autoconfigure.mqtt.proxy;

import cn.hutool.aop.ProxyUtil;
import cn.hutool.aop.aspects.SimpleAspect;
import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ReflectUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jbm.util.proxy.ReflectUtils;
import jbm.framework.boot.autoconfigure.mqtt.AbstractMqttMessageListener;
import jbm.framework.boot.autoconfigure.mqtt.IMqttMessageListener;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttCallEvent;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttRequest;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MqttCallProxyFactory {

    private Cache<String, Object> MQTTCALL_CACHE = Caffeine.newBuilder().build();
    private final RealMqttPahoClientFactory mqttPahoClientFactory;

    private MqttCallProxyFactory(RealMqttPahoClientFactory mqttPahoClientFactory) {
        this.mqttPahoClientFactory = mqttPahoClientFactory;
    }

    public <T> T getService(String name,Class<T> clazz) {
        T service = (T) MQTTCALL_CACHE.getIfPresent(clazz);
        if (service != null) {
            return service;
        }
        service = this.registerService(name,clazz);
        return service;
    }

    public <T> T registerService(String name,Class<T> clazz) {

        T bean = ReflectUtil.newInstance(clazz);
        T proxy = ProxyUtil.proxy(bean, MqttCallAspect.class);
        MQTTCALL_CACHE.put(name, proxy);
        return proxy;
    }

    /**
     * 自定义切面类，继承SimpleAspect即可
     */
    static class MqttCallAspect extends SimpleAspect {

        @Override
        public boolean before(Object target, Method method, Object[] args) {
            //继承此类后实现此方法
            return true;
        }

        @Override
        public boolean after(Object target, Method method, Object[] args, Object returnVal) {
            //继承此类后实现此方法
            return true;
        }

        @Override
        public boolean afterException(Object target, Method method, Object[] args, Throwable e) {
            //继承此类后实现此方法
            return true;
        }

    }


//    private void fromBean(Object bean) {
//        MqttCallClient mqttCallClient = AnnotationUtil.getAnnotation(bean.getClass(), MqttCallClient.class);
//        SimpleMqttClient simpleMqttClient = mqttPahoClientFactory.getClientInstance(mqttCallClient.clientId());
//    }

    public MqttCallClientBean buildRequiredBean(Object bean){
        Class<?> clazz = bean.getClass();
        MqttCallClient mqttCallClient = AnnotationUtil.getAnnotation(clazz, MqttCallClient.class);
        if (mqttCallClient == null) {
            throw new RuntimeException("未找到注解MqttCallClient");
        }
        SimpleMqttClient simpleMqttClient = mqttPahoClientFactory.getClientInstance(mqttCallClient.clientId());
        MqttCallClientBean mqttCallClientBean = new MqttCallClientBean(
                bean,
                simpleMqttClient,
                mqttCallClient.requestTopic(),
                mqttCallClient.responseTopic()
        );
        List<Method> methods = ReflectUtils.findAnnotationMethods(bean.getClass(), MqttRequest.class);
        for (Method method : methods) {
            MqttCallMethodBean mqttCallMethodBean = new MqttCallMethodBean();
            mqttCallMethodBean.setMethod(method);
//            mqttCallMethodBean.setBean(bean);
            //如果方法上有注解说明需要监听来源
            MqttCallEvent mqttRequest = AnnotationUtil.getAnnotation(method, MqttCallEvent.class);
            mqttCallMethodBean.setEvent(mqttRequest.value());
            log.debug("mqtt request [{}]", mqttCallMethodBean);

            mqttCallClientBean.getMethodMap().put(method.getName(), mqttCallMethodBean);
        }
        //到系统准备好了之后再监听
        this.subscribeMethod(mqttCallClientBean);
        return mqttCallClientBean;
    }


    /**
     * 订阅方法
     */
    public void subscribeMethod(MqttCallClientBean mqttCallClientBean) {
        mqttCallClientBean.getSimpleMqttClient()
                .subscribeWithResponse(mqttCallClientBean.getRequestTopic(),
                        new AbstractMqttMessageListener() {

                            @Override
                            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                                String methodName = mqttMessage.getPayloadStr();
                                ReflectUtil.invoke(mqttCallClientBean.getBean(), methodName);
                            }
                        });
    }

    @Data
  public static class MqttCallClientBean {
        private final Object bean;
        private final SimpleMqttClient simpleMqttClient;
        private final String requestTopic;
        private final String responseTopic;
        private Map<String,MqttCallMethodBean> methodMap = new ConcurrentHashMap<>();

        public MqttCallClientBean(Object bean, SimpleMqttClient simpleMqttClient, String requestTopic, String responseTopic) {
            this.bean = bean;
            this.simpleMqttClient = simpleMqttClient;
            this.requestTopic = requestTopic;
            this.responseTopic = responseTopic;
        }

    }

    @Data
    public  static class MqttCallMethodBean {
        private String event;
        private Method method;
//        private Object bean;
    }


}
