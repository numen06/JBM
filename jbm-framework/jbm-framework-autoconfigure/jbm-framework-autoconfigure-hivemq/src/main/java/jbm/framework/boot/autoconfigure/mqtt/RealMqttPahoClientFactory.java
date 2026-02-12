package jbm.framework.boot.autoconfigure.mqtt;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.factories.Mqtt5ClientFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wesley
 */
@Slf4j
public class RealMqttPahoClientFactory {

    private final Mqtt5ClientFactory mqtt5ClientFactory;

    private final MqttProperties mqttConnectProperties;

    private final Environment environment;

    // 客户端缓存，避免重复创建相同 Client ID 的客户端
    private final Map<String, SimpleMqttClient> clientCache = new ConcurrentHashMap<>();

    /** 可选，MQTT5 时由容器注入的共享 Mqtt5AsyncClient Bean，用于复用避免重复连接 */
    private final Mqtt5AsyncClient sharedMqtt5AsyncClient;

    public RealMqttPahoClientFactory(Mqtt5ClientFactory mqtt5ClientFactory, MqttProperties mqttConnectProperties) {
        this(mqtt5ClientFactory, mqttConnectProperties, null, null);
    }

    public RealMqttPahoClientFactory(Mqtt5ClientFactory mqtt5ClientFactory, MqttProperties mqttConnectProperties, Environment environment) {
        this(mqtt5ClientFactory, mqttConnectProperties, environment, null);
    }

    public RealMqttPahoClientFactory(Mqtt5ClientFactory mqtt5ClientFactory, MqttProperties mqttConnectProperties,
            Environment environment, @Nullable Mqtt5AsyncClient sharedMqtt5AsyncClient) {
        super();
        this.mqtt5ClientFactory = mqtt5ClientFactory;
        this.mqttConnectProperties = mqttConnectProperties;
        this.environment = environment;
        this.sharedMqtt5AsyncClient = sharedMqtt5AsyncClient;
    }

    /**
     * 生成实例唯一后缀：优先本机 IP，失败时用 8 位随机码。
     * 用于多实例部署时避免共享 clientId 冲突。
     */
    public static String generateInstanceSuffix() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            if (StrUtil.isNotBlank(ip) && !ip.startsWith("127.") && !"0.0.0.0".equals(ip)) {
                return ip.replace(".", "_");
            }
        } catch (Exception ignored) {
        }
        return IdUtil.simpleUUID().substring(0, 8);
    }

    /**
     * 获取共享 clientId：优先使用 spring.mqtt.client-id，未配置则使用 spring.application.name + 实例后缀（IP 或随机码），防止多实例冲突
     */
    public String getSharedClientId() {
        if (StrUtil.isNotBlank(mqttConnectProperties.getClientId())) {
            return mqttConnectProperties.getClientId();
        }
        String base;
        if (environment != null) {
            String appName = environment.getProperty("spring.application.name");
            if (StrUtil.isNotBlank(appName)) {
                base = appName;
            } else {
                base = "default-mqtt-client";
            }
        } else {
            base = "default-mqtt-client";
        }
        return base + "-" + generateInstanceSuffix();
    }

    @SneakyThrows
    public SimpleMqttClient getClientInstance() {
        // 默认一个程序只开一个客户端，使用共享 clientId
        return this.getClientInstance(getSharedClientId());
    }

    // 使用短UUID作为标签（去除连字符，只有8位）
    private final String mqttTag = IdUtil.simpleUUID().substring(0, 8);

    /**
     * 分布式程序的客户端创建，避免冲突（带缓存）
     *
     * @param clientId 客户端ID
     * @param tags 额外标签
     * @return MQTT客户端（如果已存在则返回缓存的实例）
     */
    public SimpleMqttClient getAppClientInstance(String clientId, Object... tags) {
        // 生成完整的 Client ID
        tags = ArrayUtil.insert(tags, 0, clientId);
        tags = ArrayUtil.append(tags, mqttTag);
        String fullClientId = StrUtil.join(StrUtil.UNDERLINE, tags);
        
        // 使用缓存，避免重复创建相同 Client ID 的客户端
        return clientCache.computeIfAbsent(fullClientId, id -> {
            log.debug("Creating MQTT client: ClientId={}", id);
            MqttProperties properties = new MqttProperties();
            BeanUtil.copyProperties(mqttConnectProperties, properties);
            properties.setClientId(id);
            Mqtt5AsyncClient mqtt5AsyncClient = mqtt5ClientFactory.mqttClient(properties, null);
            return new SimpleMqttClient(mqtt5AsyncClient, properties);
        });
    }


    @SneakyThrows
    public SimpleMqttClient getClientInstance(String clientId) {
        return clientCache.computeIfAbsent(clientId, id -> {
            if (id.equals(getSharedClientId()) && sharedMqtt5AsyncClient != null) {
                log.debug("Reusing shared Mqtt5AsyncClient bean for ClientId={}", id);
                return new SimpleMqttClient(sharedMqtt5AsyncClient, mqttConnectProperties);
            }
            log.debug("Creating MQTT client: ClientId={}", id);
            MqttProperties properties = new MqttProperties();
            BeanUtil.copyProperties(mqttConnectProperties, properties);
            properties.setClientId(id);
            Mqtt5AsyncClient mqtt5AsyncClient = mqtt5ClientFactory.mqttClient(properties, null);
            return new SimpleMqttClient(mqtt5AsyncClient, properties);
        });
    }

    @SneakyThrows
    public Mqtt5Client getClientInstance(String url, String clientId) {
        MqttProperties properties = new MqttProperties();
        BeanUtil.copyProperties(mqttConnectProperties, properties);
        if (StrUtil.isNotBlank(url)) {
            properties.setUrl(URLUtil.getStringURI(url));
        }
        if (StrUtil.isNotBlank(clientId)) {
            properties.setClientId(clientId);
        }
        return this.getClientInstance(properties);
    }

    /**
     * 通过参数直接创建MQTT对象
     *
     * @param properties
     * @return
     * @
     */
    public synchronized Mqtt5Client getClientInstance(MqttProperties properties) {
        // 创建客户端
        return mqtt5ClientFactory.mqttClient(properties, null);
    }
    
    /**
     * 获取缓存的客户端数量
     * 
     * @return 当前缓存的客户端数量
     */
    public int getCachedClientCount() {
        return clientCache.size();
    }
    
    /**
     * 清理指定的客户端缓存
     * 
     * @param clientId 客户端ID
     */
    public void removeClient(String clientId) {
        SimpleMqttClient client = clientCache.remove(clientId);
        if (client != null) {
            log.info("🗑️ Removed MQTT client from cache: ClientId={}", clientId);
            try {
                client.shutdown();
            } catch (Exception e) {
                log.warn("Failed to shutdown client: {}", clientId, e);
            }
        }
    }
    
    /**
     * 清理所有客户端缓存
     */
    public void clearAllClients() {
        log.info("🗑️ Clearing all {} cached MQTT clients", clientCache.size());
        clientCache.values().forEach(client -> {
            try {
                client.shutdown();
            } catch (Exception e) {
                log.warn("Failed to shutdown client", e);
            }
        });
        clientCache.clear();
    }
    
    /**
     * 恢复所有缓存的客户端订阅（在 MQTT 连接成功后调用）
     */
    public void restoreAllSubscriptions() {
        if (clientCache.isEmpty()) {
            return;
        }
        log.info("🔄 Restoring subscriptions for {} MQTT clients from RealMqttPahoClientFactory", clientCache.size());
        clientCache.values().forEach(client -> {
            try {
                client.restoreSubscriptions();
            } catch (Exception e) {
                log.error("❌ Failed to restore subscriptions for client", e);
            }
        });
    }

}
