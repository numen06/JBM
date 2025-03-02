package jbm.framework.boot.autoconfigure.mqtt.proxy.call;

import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallEvent;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttCallBean;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttCallContext;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
            boolean isLocal = false;
            // 方法调用前的逻辑
            log.info("Before calling method: {}", method.getName());
            try {
                MqttCallEvent mqttCallEvent = method.getAnnotation(MqttCallEvent.class);
                MqttCallContext mqttCallContext = MqttCallContextHolder.get();
                isLocal = mqttCallContext == null;
                //如果是本地请求的则没有必要构建上下文
                if (isLocal) {
//                MqttCallBean mqttCallBean = new MqttCallBean();
//                mqttCallBean.setEventCode(mqttCallEvent.value());
//                mqttCallContext = new MqttCallContext(mqttCallClient.requestTopic(), mqttCallClient.responseTopic(), mqttCallEvent.value());
//                mqttCallContext.setRequestBean(mqttCallBean);
//                MqttCallContextHolder.set(mqttCallContext);
                    // 调用原始方法
                    return callable.call();
                } else {
                    Object result = callable.call();
                    // 设置响应主题
                    String url = mqttCallClient.responseTopic();
                    // 设置响应主题
                    mqttCallContext.setResponseTopic(url);
                    // 设置返回值
                    mqttCallContext.putResponseBody(result);
                    // 发布MQTT事件
                    this.responseMqttEvent(mqttCallContext);
                    return result;
                }
            } finally {
                // 方法调用后的逻辑
                log.info("After {} calling method: {}", isLocal ? "local" : "remote", method.getName());
            }
        }
        return callable.call();
    }

    public void responseMqttEvent(MqttCallContext mqttCallContext) {
        // 发布消息到MQTT主题
        simpleMqttClient.publishObject(mqttCallContext.getResponseTopic(), mqttCallContext.getIfResponseBean());
    }


}
