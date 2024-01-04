package jbm.framework.boot.autoconfigure.mqtt.proxy;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.expression.engine.spel.SpELEngine;
import com.jbm.util.proxy.ReflectUtils;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttMapper;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttRequest;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.event.MqttMapperSubscribeEvent;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttRequsetBean;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class MqttProxyFactory implements InitializingBean, ApplicationListener<ApplicationReadyEvent> {


    private final ApplicationContext applicationContext;
    private final RealMqttPahoClientFactory mqttPahoClientFactory;


    private List<RequiredBean> requiredBeans = new ArrayList<>();

    public MqttProxyFactory(ApplicationContext applicationContext, RealMqttPahoClientFactory mqttPahoClientFactory) {
        this.applicationContext = applicationContext;
        this.mqttPahoClientFactory = mqttPahoClientFactory;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            subscribe();
            applicationContext.publishEvent(new MqttMapperSubscribeEvent(mqttPahoClientFactory));
        } catch (Exception e) {
            log.error("subscribe error", e);
        }
    }

    /**
     * 订阅方法
     */
    public void subscribe() {
        requiredBeans.forEach(requiredBean -> {
            try {
                subscribeMethod(requiredBean.mqttRequsetBean, requiredBean.simpleMqttClient);
            } catch (Exception e) {
                log.error("subscribe error", e);
            }
        });
    }

    //    public void proxySend() throws MqttException {
//        Set<Class<?>> sss = ClassUtil.scanPackageByAnnotation("com.jbm.test.mqtt.proxy", MqttMapper.class);
//        sss.forEach(clazz -> {
//            if (clazz.isInterface()) {
//                // ClassName 是 interface
//                Object obj = ProxyUtil.newProxyInstance(new MqttSendInvocationHandler(),
//                        new Class<?>[]{clazz});
//                System.out.println(obj);
//                String beanName = ClassUtil.getClassName(clazz, true);
//                SpringUtil.registerBean(beanName, obj);
//            }
//        });
//    }
    public boolean isProxyClass(Class<?> clazz) {
        // 判断是否为代理类
        if (clazz.getName().contains("Proxy")) {
            return true;
        }
        // 如果不是代理类，则递归判断父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.getName().contains("Proxy")) {
            return isProxyClass(superClass);
        }
        return false;
    }

    public void find() throws MqttException {
        Map<String, Object> mqttProxys = applicationContext.getBeansWithAnnotation(MqttMapper.class);
        for (String name : mqttProxys.keySet()) {
            log.debug("class {} find mqtt proxy", name);
            Object bean = mqttProxys.get(name);
            if (isProxyClass(bean.getClass())) {
                continue;
            }
            MqttMapper mqttMapper = AnnotationUtil.getAnnotation(bean.getClass(), MqttMapper.class);
            String clientId = StrUtil.isBlank(mqttMapper.clientId()) ? MqttProxyFactory.class.getSimpleName() + IdUtil.fastSimpleUUID() : mqttMapper.clientId();
            SimpleMqttClient simpleMqttClient = mqttPahoClientFactory.getClientInstance(clientId);

            Field[] fields = ClassUtil.getDeclaredFields(bean.getClass());
            for (Field field : fields) {
                if (field.getType().equals(SimpleMqttClient.class)) {
                    ReflectUtil.setFieldValue(bean, field, simpleMqttClient);
                }
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
                this.requiredBeans.add(new RequiredBean(simpleMqttClient, mqttRequsetBean));
                //到系统准备好了之后再监听
//                this.subscribeMethod(mqttRequsetBean, simpleMqttClient);
            }
        }
    }

    public String buildTopic(Object bean, String... url) {
        String str = StrUtil.concat(true, url);
        String[] vals = StrUtil.subBetweenAll(str, "${", "}");
        for (String val : vals) {
            String expression = StrUtil.concat(true, "#", val, "");
            String result = new SpELEngine().eval(expression, BeanUtil.beanToMap(bean), null).toString();
            str = StrUtil.replace(str, StrUtil.concat(true, "${", val, "}"), result);
        }
        return str;
    }

    public void subscribeMethod(MqttRequsetBean mqttRequsetBean, SimpleMqttClient simpleMqttClient) throws MqttException {
        log.info("start subscribe mqtt topic to method:{}", mqttRequsetBean.getRequestTopic());
        simpleMqttClient.subscribeWithResponse(mqttRequsetBean.getRequestTopic(), new MqttRequestListener(mqttRequsetBean, simpleMqttClient));
    }

    /**
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            //系统已经准备
            this.find();
//            this.proxySend();
        } catch (Exception e) {
            log.error("find mqtt proxy error", e);
        }
    }

    class RequiredBean {
        private final SimpleMqttClient simpleMqttClient;
        private final MqttRequsetBean mqttRequsetBean;

        public RequiredBean(SimpleMqttClient simpleMqttClient, MqttRequsetBean mqttRequsetBean) {
            this.simpleMqttClient = simpleMqttClient;
            this.mqttRequsetBean = mqttRequsetBean;
        }

    }
}
