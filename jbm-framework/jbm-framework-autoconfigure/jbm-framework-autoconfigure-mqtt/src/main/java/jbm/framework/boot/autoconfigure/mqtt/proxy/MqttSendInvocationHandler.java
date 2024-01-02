package jbm.framework.boot.autoconfigure.mqtt.proxy;

import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttSend;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class MqttSendInvocationHandler implements InvocationHandler {


    /**
     * @param proxy  the proxy instance that the method was invoked on
     * @param method the {@code Method} instance corresponding to
     *               the interface method invoked on the proxy instance.  The declaring
     *               class of the {@code Method} object will be the interface that
     *               the method was declared in, which may be a superinterface of the
     *               proxy interface that the proxy class inherits the method through.
     * @param args   an array of objects containing the values of the
     *               arguments passed in the method invocation on the proxy instance,
     *               or {@code null} if interface method takes no arguments.
     *               Arguments of primitive types are wrapped in instances of the
     *               appropriate primitive wrapper class, such as
     *               {@code java.lang.Integer} or {@code java.lang.Boolean}.
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (method.getName().equals("toString")) {
            return "";
        }
        // 获取方法附加注解
        MqttSend annotation = method.getAnnotation(MqttSend.class);
        return null;
    }
}
