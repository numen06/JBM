package jbm.framework.boot.autoconfigure.mqtt.proxy.call;

import com.alibaba.fastjson.JSONObject;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttBody;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallEvent;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttParam;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttCallEventBean;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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
            if (args != null && args.length > 0) {
                System.out.println("Method arguments: " +
                        String.join(", ", java.util.Arrays.stream(args).map(Object::toString).collect(Collectors.toList())));
            }
            // 调用原始方法
            Object result = callable.call();
            MqttCallEventBean mqttCallEventBean = new MqttCallEventBean();
            String url = mqttCallClient.responseTopic();
            // 发布消息到MQTT主题
            mqttCallEventBean.setTopic(url);
            mqttCallEventBean.setEventCode(mqttCallEvent.value());
            // 发布消息到MQTT主题
            mqttCallEventBean.setMessage(result);
            // 发布消息到MQTT主题
            this.responseMqttEvent(mqttCallEventBean);

            // 方法调用后的逻辑
            System.out.println("After calling method: " + method.getName());
            System.out.println("Method returned: " + result);
            return result;
        } else {
            System.out.println("No @MqttCallEvent annotation found for method: " + method.getName());
        }
        return null;
    }

    public void responseMqttEvent(MqttCallEventBean mqttCallEventBean) {
        // 发布消息到MQTT主题
        simpleMqttClient.publishObject(mqttCallEventBean.getTopic(), mqttCallEventBean);
    }


}
