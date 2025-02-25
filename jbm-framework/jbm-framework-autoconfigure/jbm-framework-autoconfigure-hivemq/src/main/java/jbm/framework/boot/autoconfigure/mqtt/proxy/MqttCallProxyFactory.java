package jbm.framework.boot.autoconfigure.mqtt.proxy;

import cn.hutool.aop.ProxyUtil;
import cn.hutool.aop.aspects.SimpleAspect;
import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ReflectUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jbm.util.proxy.ReflectUtils;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttRequest;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttRequsetBean;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.List;

@Slf4j
public class MqttCallProxyFactory {

    private Cache<Class<?>, Object> MQTTCALL_CACHE = Caffeine.newBuilder().build();
    private final RealMqttPahoClientFactory mqttPahoClientFactory;

    private MqttCallProxyFactory(RealMqttPahoClientFactory mqttPahoClientFactory) {
        this.mqttPahoClientFactory = mqttPahoClientFactory;
    }

    public <T> T getService(Class<T> clazz) {
        return (T) MQTTCALL_CACHE.getIfPresent(clazz);
    }

    public <T> T registerService(Class<T> clazz, String get, String set) {
        T bean = ReflectUtil.newInstance(clazz);
        T proxy = ProxyUtil.proxy(bean, MqttCallAspect.class);
        MQTTCALL_CACHE.put(clazz, proxy);
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


    private void fromBean(Object bean) {
        MqttCallClient mqttCallClient = AnnotationUtil.getAnnotation(bean.getClass(), MqttCallClient.class);
        SimpleMqttClient simpleMqttClient = mqttPahoClientFactory.getClientInstance(mqttCallClient.clientId());
    }

//    public void callMethod(Object bean){
//        List<Method> methods = ReflectUtils.findAnnotationMethods(bean.getClass(), MqttRequest.class);
//        for (Method method : methods) {
//            MqttRequsetBean mqttRequsetBean = new MqttRequsetBean();
//            mqttRequsetBean.setMethod(method);
//            mqttRequsetBean.setBean(bean);
//            //如果方法上有注解说明需要监听来源
//            MqttRequest mqttRequest = AnnotationUtil.getAnnotation(method, MqttRequest.class);
//            mqttRequsetBean.setRequestTopic(mqttCallClient.requestTopic());
//            mqttRequsetBean.setResponseTopic(mqttCallClient.responseTopic());
//            log.debug("mqtt request [{}]", mqttRequsetBean);
//            MQTTCALL_CACHE.put(bean.getClass(), new RequiredBean(simpleMqttClient, mqttRequsetBean));
//            //到系统准备好了之后再监听
////                this.subscribeMethod(mqttRequsetBean, simpleMqttClient);
//        }
//    }


    public void subscribeMethod(MqttRequsetBean mqttRequsetBean, SimpleMqttClient simpleMqttClient) {
        log.info("start subscribe mqtt topic to method:{}", mqttRequsetBean.getRequestTopic());
        MqttRequestListener mqttRequestListener = new MqttRequestListener(mqttRequsetBean, simpleMqttClient);
        simpleMqttClient.subscribeWithResponse(mqttRequsetBean.getRequestTopic(), mqttRequestListener);
    }

    static class RequiredBean {
        private final SimpleMqttClient simpleMqttClient;
        private final MqttRequsetBean mqttRequsetBean;

        public RequiredBean(SimpleMqttClient simpleMqttClient, MqttRequsetBean mqttRequsetBean) {
            this.simpleMqttClient = simpleMqttClient;
            this.mqttRequsetBean = mqttRequsetBean;
        }

    }


}
