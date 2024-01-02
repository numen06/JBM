package jbm.framework.boot.autoconfigure.mqtt.registrar;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttMapper;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttSend;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.proxy.MqttProxyFactory;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttResponseBean;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.cglib.proxy.InvocationHandler;

import java.io.Serializable;
import java.lang.reflect.Method;

public class MqttMapperInterfaceProxy<T> implements InvocationHandler, Serializable {

    private final Class<T> mqttMapperInterface;

    private final RealMqttPahoClientFactory mqttPahoClientFactory;

    public MqttMapperInterfaceProxy(Class<T> mqttMapper, RealMqttPahoClientFactory mqttPahoClientFactory) {
        this.mqttMapperInterface = mqttMapper;
        this.mqttPahoClientFactory = mqttPahoClientFactory;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (this.mqttMapperInterface.isAnnotationPresent(MqttMapper.class)) {
            // 读取类上注解
            MqttMapper mqttMapper = this.mqttMapperInterface.getAnnotation(MqttMapper.class);
            String clientId = StrUtil.isBlank(mqttMapper.clientId()) ? MqttProxyFactory.class.getSimpleName() + IdUtil.fastSimpleUUID() : mqttMapper.clientId();
            SimpleMqttClient simpleMqttClient = mqttPahoClientFactory.getClientInstance(clientId);
            if (method.isAnnotationPresent(MqttSend.class)) {
                // 读取方法上注解
                MqttSend methodAnnotation = method.getAnnotation(MqttSend.class);

                MqttMessage mqttMessage = new MqttMessage();
                MqttResponseBean mqttResponseBean = new MqttResponseBean(methodAnnotation.toTopic(), args[0]);
                if (ObjectUtil.isNotEmpty(mqttResponseBean.getBody()) && mqttResponseBean.getBody() instanceof String) {
                    mqttMessage.setPayload(StrUtil.bytes(mqttResponseBean.getBody().toString()));
                } else {
                    mqttMessage.setPayload(JSON.toJSONBytes(mqttResponseBean.getBody()));
                }
                simpleMqttClient.publish(methodAnnotation.toTopic(),mqttMessage );
                System.out.println("调用接口方法名:" + methodAnnotation.toTopic());
            }
        }

        return null;
    }

}
