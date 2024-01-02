package jbm.framework.boot.autoconfigure.mqtt.registrar;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.cglib.proxy.Proxy;

public class MqttMapperBeanFactory<T> implements FactoryBean<T> {

    private Class<T> ckInterface;

    public MqttMapperBeanFactory() {
    }
    public MqttMapperBeanFactory(Class<T> ckInterface) {
        this.ckInterface = ckInterface;
    }

    /**
     * bean实例化对象,指向代理类即可
     */
    @Override
    public T getObject() throws Exception {
        // 返回CkInterfaceProxy代理对象
        return (T) Proxy.newProxyInstance(ckInterface.getClassLoader(),
                new Class[]{ckInterface},
                new MqttInterfaceProxy<>(ckInterface));
    }

    /**
     * bean对象类型
     */
    @Override
    public Class<T> getObjectType() {
        return this.ckInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
