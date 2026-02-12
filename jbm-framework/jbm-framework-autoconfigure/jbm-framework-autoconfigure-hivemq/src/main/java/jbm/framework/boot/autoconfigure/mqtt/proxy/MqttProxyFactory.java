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
import jbm.framework.boot.autoconfigure.mqtt.event.MqttConnectedEvent;
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
public class MqttProxyFactory implements InitializingBean, ApplicationListener<org.springframework.context.ApplicationEvent> {


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
    
    // 标记 find() 是否已经被调用过，防止重复初始化
    private volatile boolean findCalled = false;
    
    // 标记 subscribe() 是否已经被调用过，防止重复订阅
    private volatile boolean subscribeCalled = false;

    public MqttProxyFactory(ApplicationContext applicationContext, RealMqttPahoClientFactory mqttPahoClientFactory) {
        this.applicationContext = applicationContext;
        this.mqttPahoClientFactory = mqttPahoClientFactory;
    }

    @Override
    public void onApplicationEvent(org.springframework.context.ApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent) {
            try {
                subscribe();
                applicationContext.publishEvent(new MqttMapperSubscribeEvent(mqttPahoClientFactory));
                log.info("✅ MQTT Mapper subscriptions initialized successfully, {} subscriptions registered", subscriptionMap.size());
            } catch (Exception e) {
                log.error("❌ Failed to initialize MQTT subscriptions", e);
            }
        } else if (event instanceof MqttConnectedEvent) {
            try {
                restoreAllSubscriptions();
            } catch (Exception e) {
                log.error("❌ Failed to restore subscriptions on MQTT connected", e);
            }
        }
    }

    /**
     * 订阅方法
     */
    public void subscribe() {
        // 防止 subscribe() 被多次调用，避免重复订阅
        if (subscribeCalled) {
            if (isUsingOnlySharedClient()) {
                log.debug("subscribe() 已经被调用过，跳过重复订阅（使用默认共享客户端）");
            } else {
                log.warn("⚠️ subscribe() 已经被调用过，跳过重复订阅");
            }
            return;
        }
        subscribeCalled = true;
        
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
        log.info("🔄 Restoring subscriptions for {} MQTT clients from MqttProxyFactory", clientCache.size());
        // 直接使用缓存的客户端，避免遍历 requiredBeans
        clientCache.values().forEach(client -> {
            try {
                client.restoreSubscriptions();
            } catch (Exception e) {
                log.error("❌ Failed to restore subscriptions for client", e);
            }
        });
        
        // 同时恢复 RealMqttPahoClientFactory 缓存中的所有客户端订阅
        // 这包括通过 getClientInstance() 直接获取的客户端（如 ThingModelTopicSubscriber）
        try {
            mqttPahoClientFactory.restoreAllSubscriptions();
        } catch (Exception e) {
            log.error("❌ Failed to restore subscriptions from RealMqttPahoClientFactory", e);
        }
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
     * 生成 MQTT 客户端 ID：默认使用共享 clientId（一个程序一个客户端），
     * 若 @MqttMapper(clientId="xxx") 显式指定则单独开客户端（特殊需求如协议驱动、设备代理）。
     */
    private String getMqttClientId(MqttMapper mqttMapper, Class<?> bean) {
        if (StrUtil.isNotBlank(mqttMapper.clientId())) {
            return mqttMapper.clientId();
        }
        return mqttPahoClientFactory.getSharedClientId();
    }

    /**
     * 是否仅使用默认共享客户端（用于重复类提示降级）
     */
    private boolean isUsingOnlySharedClient() {
        String sharedId = mqttPahoClientFactory.getSharedClientId();
        return clientCache.size() == 1 && clientCache.containsKey(sharedId);
    }

    /**
     * 获取或创建 MQTT 客户端（带缓存）。
     * 默认共享 clientId 时复用 getClientInstance()，不重复申请带 tag 的客户端。
     */
    private SimpleMqttClient getOrCreateClient(String clientId) {
        return clientCache.computeIfAbsent(clientId, id -> {
            if (id.equals(mqttPahoClientFactory.getSharedClientId())) {
                log.debug("Using shared MQTT client");
                return mqttPahoClientFactory.getClientInstance();
            }
            log.info("🔌 Creating new MQTT client with ID: {}", id);
            return mqttPahoClientFactory.getAppClientInstance(id);
        });
    }

    public void find() {
        // 防止重复调用 find()，避免重复注册订阅
        if (findCalled) {
            if (isUsingOnlySharedClient()) {
                log.debug("find() 已经被调用过，跳过重复初始化（使用默认共享客户端）");
            } else {
                log.debug("⚠️ find() 已经被调用过，跳过重复初始化");
            }
            return;
        }
        findCalled = true;
        
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
                    if (isUsingOnlySharedClient()) {
                        log.debug("Duplicate subscription detected for [{}].{} on topic [{}], skipping duplicate (using default shared client)",
                                bean.getClass().getSimpleName(), method.getName(), mqttRequsetBean.getRequestTopic());
                    } else {
                        log.warn("⚠️ Duplicate subscription detected for [{}].{} on topic [{}], skipping duplicate",
                                bean.getClass().getSimpleName(), method.getName(), mqttRequsetBean.getRequestTopic());
                    }
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
            String beanName = mqttRequsetBean.getBean().getClass().getSimpleName();
            String methodName = mqttRequsetBean.getMethod().getName();
            if (isUsingOnlySharedClient()) {
                log.debug("subscribeMethod 已经被调用过，跳过重复订阅 - subscriptionKey: {}, Bean: {}, Method: {} (using default shared client)",
                        subscriptionKey, beanName, methodName);
            } else {
                log.warn("⚠️ subscribeMethod 已经被调用过，跳过重复订阅 - subscriptionKey: {}, Bean: {}, Method: {}",
                        subscriptionKey, beanName, methodName);
            }
            return;
        }
        
        String clientId = simpleMqttClient.getClient().getConfig().getClientIdentifier().toString();
        String topic = mqttRequsetBean.getRequestTopic();
        String mqttSubscriptionKey = clientId + ":" + topic;
        
        // 创建当前方法的监听器
        MqttRequestListener listener = new MqttRequestListener(mqttRequsetBean, simpleMqttClient);
        String beanName = mqttRequsetBean.getBean().getClass().getSimpleName();
        String methodName = mqttRequsetBean.getMethod().getName();
        
        // 使用 computeIfAbsent 确保线程安全
        mqttSubscriptionCache.compute(mqttSubscriptionKey, (key, listeners) -> {
            if (listeners == null) {
                // 第一次订阅这个 topic，需要在 MQTT 层面订阅
                listeners = new ArrayList<>();
                listeners.add(listener);
                
                // 创建一个多播监听器，将消息分发给所有监听器
                // 注意：使用 final 变量捕获 mqttSubscriptionKey，确保在多播监听器中能正确获取监听器列表
                final String finalMqttSubscriptionKey = mqttSubscriptionKey;
                AbstractMqttMessageListener multicastListener = new AbstractMqttMessageListener() {
                    // 基于消息对象引用的去重缓存，防止同一个消息对象被处理多次
                    // key: "topic:messageIdentityHash"，value: 处理时间戳
                    private final Map<String, Long> messageObjectDeduplicationCache = new ConcurrentHashMap<>();
                    
                    @Override
                    public void messageArrived(String msgTopic, jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage message) throws Exception {
                        // 基于消息对象的引用（identityHashCode）进行去重，防止同一个消息对象被处理多次
                        // 这与消息内容去重不同：如果客户端发送两个不同的消息（即使内容相同），它们的对象引用不同，都会被处理
                        String messageIdentityHash = Integer.toHexString(System.identityHashCode(message));
                        String dedupKey = msgTopic + ":" + messageIdentityHash;
                        long currentTime = System.currentTimeMillis();
                        
                        // 检查100ms内是否已处理过该消息对象
                        Long lastProcessTime = messageObjectDeduplicationCache.get(dedupKey);
                        if (lastProcessTime != null) {
                            long timeSinceLastProcess = currentTime - lastProcessTime;
                            if (timeSinceLastProcess < 100) {
                                log.warn("⚠️ 检测到重复消息对象，跳过处理 - Topic: {}, MessageIdentityHash: {}, 距离上次处理: {}ms", 
                                        msgTopic, messageIdentityHash, timeSinceLastProcess);
                                return;
                            }
                        }
                        
                        // 记录处理时间
                        messageObjectDeduplicationCache.put(dedupKey, currentTime);
                        // 清理5秒前的条目
                        messageObjectDeduplicationCache.entrySet().removeIf(entry -> currentTime - entry.getValue() > 5000);
                        
                        List<MqttRequestListener> currentListeners = mqttSubscriptionCache.get(finalMqttSubscriptionKey);
                        if (currentListeners != null && !currentListeners.isEmpty()) {
                            if (currentListeners.size() > 1) {
                                log.warn("⚠️ 检测到多个监听器 - Topic: {}, 监听器数量: {}, 列表: {}", 
                                        msgTopic, currentListeners.size(), 
                                        currentListeners.stream()
                                                .map(l -> l.getMqttRequsetBean().getBean().getClass().getSimpleName() 
                                                        + "." + l.getMqttRequsetBean().getMethod().getName())
                                                .collect(java.util.stream.Collectors.joining(", ")));
                            }
                            for (MqttRequestListener l : currentListeners) {
                                try {
                                    l.messageArrived(msgTopic, message);
                                } catch (Exception e) {
                                    String beanName = l.getMqttRequsetBean().getBean().getClass().getSimpleName();
                                    String methodName = l.getMqttRequsetBean().getMethod().getName();
                                    log.error("多播监听器处理失败 - Topic: {}, Bean: {}, Method: {}", 
                                            msgTopic, beanName, methodName, e);
                                }
                            }
                        } else {
                            log.warn("多播监听器列表为空 - Topic: {}, mqttSubscriptionKey: {}", 
                                    msgTopic, finalMqttSubscriptionKey);
                        }
                    }
                };
                
                simpleMqttClient.subscribeWithResponse(topic, multicastListener);
                log.info("✅ MQTT订阅完成 - Topic: {}, Bean: {}, Method: {}, mqttSubscriptionKey: {}", 
                        topic, beanName, methodName, mqttSubscriptionKey);
            } else {
                // 已经订阅过了，直接添加监听器到列表
                // 检查是否已存在相同的监听器（通过 bean 和方法来判断，而不是 identityHashCode）
                boolean alreadyExists = false;
                for (MqttRequestListener existingListener : listeners) {
                    MqttRequsetBean existingBean = existingListener.getMqttRequsetBean();
                    // 通过 bean 对象和方法来判断是否为同一个监听器
                    if (existingBean.getBean() == mqttRequsetBean.getBean() 
                            && existingBean.getMethod().equals(mqttRequsetBean.getMethod())) {
                        alreadyExists = true;
                        log.warn("⚠️ 监听器已存在，跳过添加 - Topic: {}, Bean: {}, Method: {}, 当前列表大小: {}", 
                                mqttSubscriptionKey, beanName, methodName, listeners.size());
                        break;
                    }
                }
                if (!alreadyExists) {
                    listeners.add(listener);
                    log.warn("⚠️ 检测到重复订阅：监听器被添加到已订阅列表 - Topic: {}, Bean: {}, Method: {}, 列表大小: {} -> {} (这不应该发生，说明 subscribedKeys 检查可能失效)", 
                            mqttSubscriptionKey, beanName, methodName, listeners.size() - 1, listeners.size());
                }
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
