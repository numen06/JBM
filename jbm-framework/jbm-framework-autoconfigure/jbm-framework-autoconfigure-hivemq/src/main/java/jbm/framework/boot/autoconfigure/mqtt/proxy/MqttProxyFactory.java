package jbm.framework.boot.autoconfigure.mqtt.proxy;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.expression.engine.spel.SpELEngine;
import com.jbm.util.proxy.ReflectUtils;
import jbm.framework.boot.autoconfigure.mqtt.AbstractMqttMessageListener;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttMapper;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttRequest;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.event.MqttMapperSubscribeEvent;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttRequsetBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wesley
 */
@Slf4j
public class MqttProxyFactory implements InitializingBean, ApplicationListener<ApplicationReadyEvent> {


    private final ApplicationContext applicationContext;
    private final RealMqttPahoClientFactory mqttPahoClientFactory;


    // 使用 Map 存储订阅，key 为 "clientId:topic:beanClass:methodName"，每个类的每个方法都是独立的订阅
    private final Map<String, RequiredBean> subscriptionMap = new ConcurrentHashMap<>();
    
    // 客户端缓存，避免重复创建相同 Client ID 的客户端
    private final Map<String, SimpleMqttClient> clientCache = new ConcurrentHashMap<>();
    
    // MQTT 订阅缓存，key 为 "clientId:topic"，确保同一个topic只在MQTT层面订阅一次
    private final Map<String, List<MqttRequestListener>> mqttSubscriptionCache = new ConcurrentHashMap<>();
    
    // 记录已经调用过 subscribeMethod 的 subscriptionKey，防止重复调用
    private final Set<String> subscribedKeys = ConcurrentHashMap.newKeySet();

    public MqttProxyFactory(ApplicationContext applicationContext, RealMqttPahoClientFactory mqttPahoClientFactory) {
        this.applicationContext = applicationContext;
        this.mqttPahoClientFactory = mqttPahoClientFactory;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            subscribe();
            applicationContext.publishEvent(new MqttMapperSubscribeEvent(mqttPahoClientFactory));
            log.info("✅ MQTT Mapper subscriptions initialized successfully, {} subscriptions registered", subscriptionMap.size());
        } catch (Exception e) {
            log.error("❌ Failed to initialize MQTT subscriptions", e);
        }
    }

    /**
     * 订阅方法
     */
    public void subscribe() {
        log.info("📡 Subscribing to {} MQTT topics", subscriptionMap.size());
        subscriptionMap.forEach((subscriptionKey, requiredBean) -> {
            try {
                subscribeMethod(subscriptionKey, requiredBean.mqttRequsetBean, requiredBean.simpleMqttClient);
            } catch (Exception e) {
                log.error("❌ Failed to subscribe to topic: {}", 
                        requiredBean.mqttRequsetBean.getRequestTopic(), e);
            }
        });
    }

    /**
     * 恢复所有客户端的订阅（在重连后调用）
     */
    public void restoreAllSubscriptions() {
        log.info("🔄 Restoring subscriptions for {} MQTT clients", clientCache.size());
        // 直接使用缓存的客户端，避免遍历 requiredBeans
        clientCache.values().forEach(client -> {
            try {
                client.restoreSubscriptions();
            } catch (Exception e) {
                log.error("❌ Failed to restore subscriptions for client", e);
            }
        });
    }

    /**
     * 获取所有缓存的客户端
     */
    public Collection<SimpleMqttClient> getAllClients() {
        return clientCache.values();
    }

    /**
     * 获取客户端统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClients", clientCache.size());
        stats.put("totalSubscriptions", subscriptionMap.size());
        stats.put("connectedClients", clientCache.values().stream()
                .filter(SimpleMqttClient::isConnected)
                .count());
        return stats;
    }

    //    public void proxySend() throws MqttException {
//        Set<Class<?>> sss = ClassUtil.scanPackageByAnnotation("com.jbm.test.mqtt.proxy", MqttMapper.class);
//        sss.forEach(clazz -> {
//            if (clazz.isInterface()) {
//                // ClassName 是 interface
//                Object obj = ProxyUtil.newProxyInstance(new MqttSendInvocationHandler(),
//                        new Class<?>[]{clazz});
//                System.out.println(obj);
//                String beanName = ClassUtil.getClassName(clazz, true);
//                SpringUtil.registerBean(beanName, obj);
//            }
//        });
//    }
    public boolean isProxyClass(Class<?> clazz) {
        // 判断是否为代理类
        if (clazz.getName().contains("Proxy")) {
            return true;
        }
        // 如果不是代理类，则递归判断父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.getName().contains("Proxy")) {
            return isProxyClass(superClass);
        }
        return false;
    }

    /**
     * 生成当前程序唯一的MQTT客户端ID
     *
     * @param mqttMapper
     * @return
     */
    private String getMqttClientId(MqttMapper mqttMapper, Class<?> bean) {
        if (StrUtil.isNotBlank(mqttMapper.clientId())) {
            return mqttMapper.clientId();
        }
        return bean.getSimpleName();
    }

    /**
     * 获取或创建 MQTT 客户端（带缓存）
     */
    private SimpleMqttClient getOrCreateClient(String clientId) {
        return clientCache.computeIfAbsent(clientId, id -> {
            log.info("🔌 Creating new MQTT client with ID: {}", id);
            return mqttPahoClientFactory.getAppClientInstance(id);
        });
    }

    public void find() {
        Map<String, Object> mqttProxys = applicationContext.getBeansWithAnnotation(MqttMapper.class);
        log.info("🔍 Found {} beans with @MqttMapper annotation", mqttProxys.size());

        for (String name : mqttProxys.keySet()) {
            log.debug("class {} find mqtt proxy", name);
            Object bean = mqttProxys.get(name);
            if (isProxyClass(bean.getClass())) {
                continue;
            }
            MqttMapper mqttMapper = AnnotationUtil.getAnnotation(bean.getClass(), MqttMapper.class);
            String clientId = getMqttClientId(mqttMapper, bean.getClass());

            // 使用缓存获取客户端，避免重复创建
            SimpleMqttClient simpleMqttClient = getOrCreateClient(clientId);
            log.debug("📱 Using MQTT client [{}] for mapper [{}]", clientId, bean.getClass().getSimpleName());

            // 注入客户端到 Bean 的字段中
            Field[] fields = ClassUtil.getDeclaredFields(bean.getClass());
            for (Field field : fields) {
                if (field.getType().equals(SimpleMqttClient.class)) {
                    ReflectUtil.setFieldValue(bean, field, simpleMqttClient);
                }
            }

            // 处理所有带 @MqttRequest 注解的方法
            List<Method> methods = ReflectUtils.findAnnotationMethods(bean.getClass(), MqttRequest.class);
            log.debug("📋 Found {} methods with @MqttRequest in {}", methods.size(), bean.getClass().getSimpleName());

            for (Method method : methods) {
                MqttRequsetBean mqttRequsetBean = new MqttRequsetBean();
                mqttRequsetBean.setMethod(method);
                mqttRequsetBean.setBean(bean);
                //如果方法上有注解说明需要监听来源
                MqttRequest mqttRequest = AnnotationUtil.getAnnotation(method, MqttRequest.class);
                mqttRequsetBean.setRequestTopic(this.buildTopic(bean, mqttMapper.value(), mqttRequest.fromTopic()));
                mqttRequsetBean.setResponseTopic(this.buildTopic(bean, mqttMapper.value(), mqttRequest.toTopic()));
                
                // 使用 clientId + topic + beanClass + methodName 作为唯一标识
                // 确保不同类的不同方法即使监听相同topic也能独立注册
                String subscriptionKey = clientId + ":" + mqttRequsetBean.getRequestTopic() + ":" 
                        + bean.getClass().getName() + ":" + method.getName();
                
                // 使用 putIfAbsent 确保同一个类的同一个方法不会重复注册
                RequiredBean existingBean = subscriptionMap.putIfAbsent(subscriptionKey, 
                        new RequiredBean(simpleMqttClient, mqttRequsetBean));
                
                if (existingBean != null) {
                    log.warn("⚠️ Duplicate subscription detected for [{}].{} on topic [{}], skipping duplicate",
                            bean.getClass().getSimpleName(), method.getName(), mqttRequsetBean.getRequestTopic());
                } else {
                    log.debug("✅ Registered subscription: [{}].{} -> {}", 
                            bean.getClass().getSimpleName(), method.getName(), mqttRequsetBean.getRequestTopic());
                }
                
//                MqttResponse mqttResponse = AnnotationUtil.getAnnotation(method, MqttResponse.class);
//                if (mqttResponse != null)
//                    mqttRequsetBean.setResponseTopic(mqttResponse.topic());
                //到系统准备好了之后再监听
//                this.subscribeMethod(mqttRequsetBean, simpleMqttClient);
            }
        }

        log.info("✅ MQTT Proxy initialization completed: {} clients created, {} subscriptions registered",
                clientCache.size(), subscriptionMap.size());
    }

    public String buildTopic(Object bean, String... url) {
        String str = StrUtil.concat(true, url);
        String[] vals = StrUtil.subBetweenAll(str, "${", "}");
        for (String val : vals) {
            String expression = StrUtil.concat(true, "#", val, "");
            String result = new SpELEngine().eval(expression, BeanUtil.beanToMap(bean), null).toString();
            str = StrUtil.replace(str, StrUtil.concat(true, "${", val, "}"), result);
        }
        return str;
    }

    public void subscribeMethod(String subscriptionKey, MqttRequsetBean mqttRequsetBean, SimpleMqttClient simpleMqttClient) {
        // 检查是否已经调用过此方法（防止 subscribe() 被多次调用导致重复添加监听器）
        if (!subscribedKeys.add(subscriptionKey)) {
            log.debug("⚠️ subscribeMethod 已经被调用过: {}, 跳过", subscriptionKey);
            return;
        }
        
        String clientId = simpleMqttClient.getClient().getConfig().getClientIdentifier().toString();
        String topic = mqttRequsetBean.getRequestTopic();
        String mqttSubscriptionKey = clientId + ":" + topic;
        
        // 创建当前方法的监听器
        MqttRequestListener listener = new MqttRequestListener(mqttRequsetBean, simpleMqttClient);
        
        // 使用 computeIfAbsent 确保线程安全
        mqttSubscriptionCache.compute(mqttSubscriptionKey, (key, listeners) -> {
            if (listeners == null) {
                // 第一次订阅这个 topic，需要在 MQTT 层面订阅
                listeners = new ArrayList<>();
                listeners.add(listener);
                
                // 创建一个多播监听器，将消息分发给所有监听器
                AbstractMqttMessageListener multicastListener = new AbstractMqttMessageListener() {
                    @Override
                    public void messageArrived(String msgTopic, jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage message) throws Exception {
                        List<MqttRequestListener> currentListeners = mqttSubscriptionCache.get(mqttSubscriptionKey);
                        if (currentListeners != null) {
                            log.debug("📨 收到消息 Topic: {}, 分发给 {} 个监听器", msgTopic, currentListeners.size());
                            for (MqttRequestListener l : currentListeners) {
                                try {
                                    l.messageArrived(msgTopic, message);
                                } catch (Exception e) {
                                    log.error("监听器处理消息失败: {}.{}", 
                                            l.getMqttRequsetBean().getBean().getClass().getSimpleName(),
                                            l.getMqttRequsetBean().getMethod().getName(), e);
                                }
                            }
                        }
                    }
                };
                
                simpleMqttClient.subscribeWithResponse(topic, multicastListener);
                log.info("📬 MQTT层订阅 Topic: {} (第1个监听器: [{}].{})",
                        topic,
                        mqttRequsetBean.getBean().getClass().getSimpleName(),
                        mqttRequsetBean.getMethod().getName());
            } else {
                // 已经订阅过了，直接添加监听器到列表
                // 注意：由于 subscribedKeys 已经防止了重复调用，这里不会重复添加
                listeners.add(listener);
                log.info("📬 添加监听器到已订阅的Topic: {} (第{}个监听器: [{}].{})",
                        topic,
                        listeners.size(),
                        mqttRequsetBean.getBean().getClass().getSimpleName(),
                        mqttRequsetBean.getMethod().getName());
            }
            return listeners;
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            //系统已经准备
            this.find();
//            this.proxySend();
        } catch (Exception e) {
            log.error("find mqtt proxy error", e);
        }
    }

    static class RequiredBean {
        private final SimpleMqttClient simpleMqttClient;
        private final MqttRequsetBean mqttRequsetBean;

        public RequiredBean(SimpleMqttClient simpleMqttClient, MqttRequsetBean mqttRequsetBean) {
            this.simpleMqttClient = simpleMqttClient;
            this.mqttRequsetBean = mqttRequsetBean;
        }

    }
}
