package jbm.framework.boot.autoconfigure.mqtt.proxy.call;

import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallEvent;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttCallBean;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttCallContext;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * 拦截器类，用于处理带有 @MqttCallEvent 注解的方法调用。
 * @author wesley
 */
public class MqttCallServiceInterceptor {
    private final SimpleMqttClient simpleMqttClient;
    private final MqttCallClient mqttCallClient;

    public MqttCallServiceInterceptor(SimpleMqttClient simpleMqttClient, MqttCallClient mqttCallClient) {
        this.simpleMqttClient = simpleMqttClient;
        this.mqttCallClient = mqttCallClient;
    }

    @RuntimeType
    public Object intercept(
            @Origin Method method,
            @AllArguments Object[] args,
            @SuperCall Callable<Object> callable) throws Exception {
        if (method.isAnnotationPresent(MqttCallEvent.class)) {
            MqttCallEvent mqttCallEvent = method.getAnnotation(MqttCallEvent.class);
            // 方法调用前的逻辑
            System.out.println("Before calling method: " + method.getName());
            MqttCallContext mqttCallContext = MqttCallContextHolder.get();
            // 调用原始方法
            Object result = callable.call();
            // 构建MQTT消息内容
            String url = mqttCallClient.responseTopic();
            mqttCallContext.setResponseTopic(url);
            // 构建MQTT消息内容
            mqttCallContext.putResponseBody(result);
            this.responseMqttEvent(mqttCallContext);
            // 方法调用后的逻辑
            System.out.println("After calling method: " + method.getName());
            return result;
        }
        return callable.call();
    }

    public void responseMqttEvent(MqttCallContext mqttCallContext) {
        // 发布消息到MQTT主题
        simpleMqttClient.publishObject(mqttCallContext.getResponseTopic(), mqttCallContext.getIfResponseBean());
    }


}
