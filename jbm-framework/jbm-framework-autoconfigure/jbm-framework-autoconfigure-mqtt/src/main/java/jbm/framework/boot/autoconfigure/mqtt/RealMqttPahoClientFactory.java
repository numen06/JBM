package jbm.framework.boot.autoconfigure.mqtt;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.util.BeanUtils;
import jbm.framework.boot.autoconfigure.mqtt.callback.SimpleMqttAsyncClientCallback;
import jbm.framework.boot.autoconfigure.mqtt.callback.SimpleMqttCallback;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttAsyncClient;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.mqtt.core.ConsumerStopAction;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

import javax.annotation.Resource;

@Slf4j
public class RealMqttPahoClientFactory extends DefaultMqttPahoClientFactory {

    private MqttConnectProperties mqttConnectProperties;

//    private LoadingCache<String, SimpleMqttPahoMessageHandler> mqttPahoClientCache = CacheBuilder.newBuilder().maximumSize(100)
//            .build(new CacheLoader<String, SimpleMqttPahoMessageHandler>() {
//                @Override
//                public SimpleMqttPahoMessageHandler load(String key) throws Exception {
//                    KeySerialization keySerialization = JSON.parseObject(key, KeySerialization.class);
//                    SimpleMqttPahoMessageHandler messageHandler = new SimpleMqttPahoMessageHandler(keySerialization.getClientId(), mqttPahoClientFactory());
//                    messageHandler.setAsync(false);
//                    messageHandler.setDefaultTopic(keySerialization.getDefaultTopic());
//                    messageHandler.setDefaultQos(0);
//                    messageHandler.setConverter(new DefaultPahoMessageConverter());
////				return  new SimpleMqttPahoMessageHandler(messageHandler);
//                    return messageHandler;
//                }
//            });
    @Resource
    private ApplicationContext applicationContext;
    private Cache<String, AutoCloseable> CLIENT_CACHE = CacheUtil.newLFUCache(100);


    public RealMqttPahoClientFactory(MqttConnectProperties mqttConnectProperties) {
        super();
        this.mqttConnectProperties = mqttConnectProperties;
        this.setConnectionOptions(mqttConnectProperties.toMqttConnectOptions());
        this.setConsumerStopAction(ConsumerStopAction.UNSUBSCRIBE_NEVER);
        this.setPersistence(new MemoryPersistence());
        //设置为文件缓存
        //this.setPersistence(new MqttDefaultFilePersistence("mqtt_paho/"));
    }

    private MqttPahoClientFactory mqttPahoClientFactory() {
        return this;
    }

    @Override
    public MqttConnectOptions getConnectionOptions() {
        return mqttConnectProperties.toMqttConnectOptions();
    }

    @SneakyThrows
    public SimpleMqttClient getClientInstance() throws MqttException {
        MqttConnectProperties properties = BeanUtils.cloneJavaBean(mqttConnectProperties);
        IMqttClient client = this.getClientInstance(properties);
        SimpleMqttClient simpleMqttClient = new SimpleMqttClient(client, properties);
        return simpleMqttClient;
    }

    @SneakyThrows
    public SimpleMqttClient getClientInstance(String clientId) throws MqttException {
        MqttConnectProperties properties = BeanUtils.cloneJavaBean(mqttConnectProperties);
        properties.setClientId(clientId);
        IMqttClient client = this.getClientInstance(properties);
        SimpleMqttClient simpleMqttClient = new SimpleMqttClient(client, properties);
        return simpleMqttClient;
    }

    @Override
    @SneakyThrows
    public IMqttClient getClientInstance(String uri, String clientId) throws MqttException {
        MqttConnectProperties properties = BeanUtils.cloneJavaBean(mqttConnectProperties);
        if (StrUtil.isNotBlank(uri))
            properties.setUrl(uri);
        if (StrUtil.isNotBlank(clientId))
            properties.setClientId(clientId);
        return this.getClientInstance(properties);
    }

    /**
     * 通过参数直接创建MQTT对象
     *
     * @param properties
     * @return
     * @throws MqttException
     */
    public synchronized IMqttClient getClientInstance(MqttConnectProperties properties) throws MqttException {
        if (CLIENT_CACHE.containsKey(properties.getClientId())) {
            return (IMqttClient) CLIENT_CACHE.get(properties.getClientId());
        }
        IMqttClient client = super.getClientInstance(properties.getUrl(), properties.getClientId());
        MqttConnectOptions mqttConnectOptions = properties.toMqttConnectOptions();
        log.info("{}", mqttConnectOptions);
        client.setCallback(new SimpleMqttCallback(client));
//        client.connect(mqttConnectOptions);
        CLIENT_CACHE.put(properties.getClientId(), client);
        return client;
    }

    @SneakyThrows
    public SimpleMqttAsyncClient getAsyncClientInstance() throws MqttException {
        MqttConnectProperties properties = BeanUtils.cloneJavaBean(mqttConnectProperties);
        IMqttAsyncClient client = this.getAsyncClientInstance(properties);

        client.connect(properties.toMqttConnectOptions());
        return new SimpleMqttAsyncClient(client);
    }

    @SneakyThrows
    public SimpleMqttAsyncClient getAsyncClientInstance(String clientId) throws MqttException {
        MqttConnectProperties properties = BeanUtils.cloneJavaBean(mqttConnectProperties);
        properties.setClientId(clientId);
        IMqttAsyncClient client = this.getAsyncClientInstance(properties);
        client.connect(properties.toMqttConnectOptions());
        return new SimpleMqttAsyncClient(client);
    }

    @SneakyThrows
    @Override
    public IMqttAsyncClient getAsyncClientInstance(String uri, String clientId) throws MqttException {
        MqttConnectProperties properties = BeanUtils.cloneJavaBean(mqttConnectProperties);
        if (StrUtil.isNotBlank(uri))
            properties.setUrl(uri);
        if (StrUtil.isNotBlank(clientId))
            properties.setClientId(clientId);
        return this.getAsyncClientInstance(properties);
    }

    /**
     * 通过参数直接创建MQTT对象
     *
     * @param properties
     * @return
     * @throws MqttException
     */
    public synchronized IMqttAsyncClient getAsyncClientInstance(MqttConnectProperties properties) throws MqttException {
        if (CLIENT_CACHE.containsKey(properties.getClientId())) {
            return (IMqttAsyncClient) CLIENT_CACHE.get(properties.getClientId());
        }
        IMqttAsyncClient client = super.getAsyncClientInstance(properties.getUrl(), properties.getClientId());
        MqttConnectOptions mqttConnectOptions = properties.toMqttConnectOptions();
        client.setCallback(new SimpleMqttAsyncClientCallback(client));
//        client.connect(mqttConnectOptions);
        CLIENT_CACHE.put(properties.getClientId(), client);
        return client;
    }

//    public SimpleMqttPahoMessageHandler cteateSimpleMqttPahoMessageHandler() {
//        return cteateSimpleMqttPahoMessageHandler(mqttConnectProperties.getClientId(), MqttHeaders.TOPIC);
//    }
//
//    public SimpleMqttPahoMessageHandler cteateSimpleMqttPahoMessageHandler(String defaultTopic) {
//        return cteateSimpleMqttPahoMessageHandler(mqttConnectProperties.getClientId(), defaultTopic);
//    }
//
//    public SimpleMqttPahoMessageHandler cteateSimpleMqttPahoMessageHandler(String clientId, String defaultTopic) {
//        log.info("create client:{}", clientId);
//        KeySerialization keySerialization = new KeySerialization(clientId, defaultTopic);
//        String key = JSON.toJSONString(keySerialization);
//        try {
//            return mqttPahoClientCache.get(key);
//        } catch (ExecutionException e) {
//            return null;
//        }
//    }
}
