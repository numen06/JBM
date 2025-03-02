package jbm.framework.boot.autoconfigure.mqtt.proxy.call;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.jbm.util.proxy.ReflectUtils;
import jbm.framework.boot.autoconfigure.mqtt.AbstractMqttMessageListener;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.*;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttCallEventBean;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wesley
 */
@Slf4j
public class MqttSubscribeProxy {

    private final SimpleMqttClient simpleMqttClient;

    private final Class<?> clazz;
    private final MqttCallClient mqttCallClient;
    private final Object proxy;

    private MqttCallClientBean mqttCallClientBean   ;

    public MqttSubscribeProxy(SimpleMqttClient simpleMqttClient, Class<?> clazz, MqttCallClient mqttCallClient, Object proxy) {
        this.simpleMqttClient = simpleMqttClient;
        this.clazz = clazz;
        this.mqttCallClient = mqttCallClient;
        this.proxy = proxy;
        this.buildRequiredBean();
        this.subscribeMethod();
    }

    public void buildRequiredBean() {
        this.mqttCallClientBean = new MqttCallClientBean(
                proxy,
                simpleMqttClient,
                mqttCallClient.requestTopic(),
                mqttCallClient.responseTopic()
        );
        List<Method> methods = ReflectUtils.findAnnotationMethods(clazz, MqttCallEvent.class);
        for (Method method : methods) {
            MqttCallMethodBean mqttCallMethodBean = new MqttCallMethodBean();
            mqttCallMethodBean.setMethod(method);
//            mqttCallMethodBean.setBean(bean);
            //如果方法上有注解说明需要监听来源
            MqttCallEvent mqttRequest = AnnotationUtil.getAnnotation(method, MqttCallEvent.class);
            mqttCallMethodBean.setEventCode(mqttRequest.value());
            log.debug("mqtt request [{}]", mqttCallMethodBean);
            mqttCallClientBean.getMethodMap().put(mqttCallMethodBean.getEventCode(), mqttCallMethodBean);
        }
    }


    /**
     * 订阅方法
     */
    public void subscribeMethod() {
        mqttCallClientBean.getSimpleMqttClient()
                .subscribeWithResponse(mqttCallClientBean.getRequestTopic(),
                        new AbstractMqttMessageListener() {
                            @Override
                            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                                //解析事件
                                String body = mqttMessage.getPayloadStr();
                                //事件bean
                                JSONObject jsonBody = JSONObject.parseObject(body);
                                MqttCallEventBean mqttCallEventBean = jsonBody.toJavaObject(MqttCallEventBean.class);
                                mqttCallEventBean.setTopic(topic);
                                //获取对应的方法
                                MqttCallMethodBean mqttCallMethodBean = mqttCallClientBean.getMethodMap().get(mqttCallEventBean.getEventCode());
                                // 提取带有注解的参数
                                Map<String, Object> params = extractParameters(mqttCallMethodBean.getMethod(),  mqttCallEventBean, jsonBody);
                                Object[] args = new Object[mqttCallMethodBean.getMethod().getParameterCount()];
                                // 将参数值填充到方法参数数组中
                                fillArguments(args,  params);
                                //执行方法
                                ReflectUtil.invoke(mqttCallClientBean.getBean(), mqttCallMethodBean.getMethod(), args);
                            }
                        });
    }

    private Map<String, Object> extractParameters(Method method,MqttCallEventBean mqttCallEventBean, JSONObject jsonBody) {
        Map<String, Object> params = new HashMap<>();
        if (mqttCallEventBean.getMessage() == null) {
            return params;
        }
        // 使用 FastJSON 解析请求体为 JSONObject
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Annotation[] annotations = parameter.getAnnotations();
            // 处理 @MqttBody 注解
            if (isAnnotatedWith(annotations, MqttBody.class)) {
                Object body = jsonBody.getObject("message", parameter.getType());
                params.put("arg" + i, body);
            }
            // 处理 @MqttParam 注解
            else if (isAnnotatedWith(annotations, MqttParam.class)) {
                JSONObject jsonMessage = jsonBody.getJSONObject("message");
                MqttParam mqttParam = parameter.getAnnotation(MqttParam.class);
                String key = mqttParam.value();
                Object value = jsonMessage.getObject(key, parameter.getType());
                params.put("arg" + i, value);
            }
        }
        return params;
    }

    private boolean isAnnotatedWith(Annotation[] annotations, Class<? extends Annotation> annotationClass) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(annotationClass)) {
                return true;
            }
        }
        return false;
    }
    private void fillArguments(Object[] args, Map<String, Object> params) {
        for (int i = 0; i < args.length; i++) {
            args[i] = params.getOrDefault("arg" + i, null);
        }
    }

    @Data
    public static class MqttCallClientBean {
        private final Object bean;
        private final SimpleMqttClient simpleMqttClient;
        private final String requestTopic;
        private final String responseTopic;
        private Map<String, MqttCallMethodBean> methodMap = new ConcurrentHashMap<>();

        public MqttCallClientBean(Object bean, SimpleMqttClient simpleMqttClient, String requestTopic, String responseTopic) {
            this.bean = bean;
            this.simpleMqttClient = simpleMqttClient;
            this.requestTopic = requestTopic;
            this.responseTopic = responseTopic;
        }

    }

    @Data
    public static class MqttCallMethodBean {
        private String eventCode;
        private Method method;
//        private Object bean;
    }
}
