package jbm.framework.boot.autoconfigure.mqtt.proxy;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.expression.engine.spel.SpELEngine;
import com.jbm.util.proxy.ReflectUtils;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wesley
 */
@Slf4j
public class MqttProxyFactory implements InitializingBean, ApplicationListener<ApplicationReadyEvent> {


    private final ApplicationContext applicationContext;
    private final RealMqttPahoClientFactory mqttPahoClientFactory;


    // 使用 Map 存储订阅，key 为 "clientId:topic"，避免重复订阅
    private final Map<String, RequiredBean> subscriptionMap = new ConcurrentHashMap<>();
    
    // 客户端缓存，避免重复创建相同 Client ID 的客户端
    private final Map<String, SimpleMqttClient> clientCache = new ConcurrentHashMap<>();

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
        subscriptionMap.values().forEach(requiredBean -> {
            try {
                subscribeMethod(requiredBean.mqttRequsetBean, requiredBean.simpleMqttClient);
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
                
                // 使用 clientId + topic 作为唯一标识，避免重复订阅
                String subscriptionKey = clientId + ":" + mqttRequsetBean.getRequestTopic();
                
                // 使用 putIfAbsent 确保同一主题只订阅一次
                RequiredBean existingBean = subscriptionMap.putIfAbsent(subscriptionKey, 
                        new RequiredBean(simpleMqttClient, mqttRequsetBean));
                
                if (existingBean != null) {
                    log.warn("⚠️ Duplicate subscription detected for topic [{}] on client [{}], skipping duplicate",
                            mqttRequsetBean.getRequestTopic(), clientId);
                } else {
                    log.debug("✅ Registered subscription: {}", subscriptionKey);
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

    public void subscribeMethod(MqttRequsetBean mqttRequsetBean, SimpleMqttClient simpleMqttClient) {
        log.info("📬 Subscribing MQTT topic [{}] to method [{}]", 
                mqttRequsetBean.getRequestTopic(), 
                mqttRequsetBean.getMethod().getName());
        MqttRequestListener mqttRequestListener = new MqttRequestListener(mqttRequsetBean, simpleMqttClient);
        simpleMqttClient.subscribeWithResponse(mqttRequsetBean.getRequestTopic(), mqttRequestListener);
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
