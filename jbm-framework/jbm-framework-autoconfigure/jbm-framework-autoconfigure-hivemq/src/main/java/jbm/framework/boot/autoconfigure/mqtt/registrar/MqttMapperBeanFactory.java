package jbm.framework.boot.autoconfigure.mqtt.registrar;

import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttRequsetBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

public class MqttMapperBeanFactory<T> implements FactoryBean<T> {


    private Class<T> mqttMapper;

    @Resource

    private RealMqttPahoClientFactory mqttPahoClientFactory;

    public MqttMapperBeanFactory() {
    }
    public MqttMapperBeanFactory(Class<T> mqttMapper) {
        this.mqttMapper = mqttMapper;
    }


    /**
     * bean实例化对象,指向代理类即可
     */
    @Override
    public T getObject() throws Exception {
        // 返回MqttMapperProxy代理对象
        return (T) Proxy.newProxyInstance(mqttMapper.getClassLoader(),
                new Class[]{mqttMapper},
                new MqttMapperInterfaceProxy<>(mqttMapper, mqttPahoClientFactory));
    }

    /**
     * bean对象类型
     */
    @Override
    public Class<T> getObjectType() {
        return this.mqttMapper;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
