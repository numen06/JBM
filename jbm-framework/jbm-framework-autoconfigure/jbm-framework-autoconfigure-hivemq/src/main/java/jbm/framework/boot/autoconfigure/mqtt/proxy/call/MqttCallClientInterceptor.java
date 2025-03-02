package jbm.framework.boot.autoconfigure.mqtt.proxy.call;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.IdUtil;
import com.jbm.util.BeanUtils;
import com.jbm.util.Callback;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallEvent;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttParam;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttCallContext;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 拦截器类，用于处理带有 @MqttCallEvent 注解的方法调用。
 * @author wesley
 */
@Slf4j
public class MqttCallClientInterceptor {
    private final SimpleMqttClient simpleMqttClient;
    private final MqttCallClient mqttCallClient;

    public MqttCallClientInterceptor(SimpleMqttClient simpleMqttClient, MqttCallClient mqttCallClient) {
        this.simpleMqttClient = simpleMqttClient;
        this.mqttCallClient = mqttCallClient;
    }

    @RuntimeType
    public Object intercept(
            @Origin Method method,
            @AllArguments Object[] args) throws Exception {
        // 检查方法是否有 @MqttCallEvent 注解
        if (method.isAnnotationPresent(MqttCallEvent.class)) {
            MqttCallEvent mqttCallEvent = method.getAnnotation(MqttCallEvent.class);
            String url = mqttCallClient.requestTopic();
            System.out.println("Calling external URL: " + url);
            // 提取带有 @MqttParam 注解的参数
            Object params = extractParameters(method, args);
            MqttCallContext mqttCallContext = new MqttCallContext(IdUtil.simpleUUID(), mqttCallEvent.value());
            MqttCallContextHolder.set(mqttCallContext);
            // 发布消息到MQTT主题
            mqttCallContext.setRequestTopic(url);
            // 发布消息到MQTT主题
            mqttCallContext.putRequestMessage(params);
            return requestMqttEvent(mqttCallContext, method);
        }
        return null;
    }

    public Object requestMqttEvent(MqttCallContext mqttCallContext, Method method) {
        // 发布消息到MQTT主题
        log.info("Sending MQTT event: {}", mqttCallContext.getEventId());
        simpleMqttClient.publishObject(mqttCallContext.getRequestTopic(), mqttCallContext.getRequestBean());
        try {
            mqttCallContext.getLatch().await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {

        }
        return Callback.call(() -> {
            if (mqttCallContext.getResponseBean() == null) {
                return null;
            }
            if (mqttCallContext.getResponseBean().getMessage() == null) {
                return null;
            }
            if (mqttCallContext.getResponseBean().getMessage().getClass() ==  method.getReturnType()){
                return mqttCallContext.getResponseBean().getMessage();
            }
            return new cn.hutool.json.JSONObject(mqttCallContext.getRequestBean()).get("message", method.getReturnType());
        });
    }

    private Object extractParameters(Method method, Object[] args) {
        Map<String, Object> params = new HashMap<>();
        if (args.length == 0) {
            return null;
        }
        if (args.length == 1) {
            return args[0];
        }
        // 遍历方法的参数注解，找到带有 @MqttParam 的参数并提取其值（如果有的话
        for (int i = 0; i < method.getParameterAnnotations().length; i++) {
            for (Annotation annotation : method.getParameterAnnotations()[i]) {
                if (annotation instanceof MqttParam) {
                    params.put(((MqttParam) annotation).value(), args[i]);
                }
            }
        }
        return params;
    }
}