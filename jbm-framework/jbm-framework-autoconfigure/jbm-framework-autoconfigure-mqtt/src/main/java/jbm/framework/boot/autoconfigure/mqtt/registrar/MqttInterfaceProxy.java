package jbm.framework.boot.autoconfigure.mqtt.registrar;

import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttMapper;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttSend;
import org.springframework.cglib.proxy.InvocationHandler;

import java.io.Serializable;
import java.lang.reflect.Method;

public class MqttInterfaceProxy<T> implements InvocationHandler, Serializable {

    private final Class<T> ckInterface;

    public MqttInterfaceProxy(Class<T> ckInterface) {
        this.ckInterface = ckInterface;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (this.ckInterface.isAnnotationPresent(MqttMapper.class)) {
            // 读取类上注解
            MqttMapper interfaceAnnotation = this.ckInterface.getAnnotation(MqttMapper.class);
            System.out.println("调用接口类名:" + interfaceAnnotation.value());
            if (method.isAnnotationPresent(MqttSend.class)) {
                // 读取方法上注解
                MqttSend methodAnnotation = method.getAnnotation(MqttSend.class);
                System.out.println("调用接口方法名:" + methodAnnotation.toTopic());
            }
        }

        return null;
    }

}
