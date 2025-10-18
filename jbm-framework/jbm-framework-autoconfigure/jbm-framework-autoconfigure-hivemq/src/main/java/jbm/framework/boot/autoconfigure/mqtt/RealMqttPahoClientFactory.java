package jbm.framework.boot.autoconfigure.mqtt;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.jbm.util.BeanUtils;
import com.jbm.util.StringUtils;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.factories.Mqtt5ClientFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wesley
 */
@Slf4j
public class RealMqttPahoClientFactory {

    private final Mqtt5ClientFactory mqtt5ClientFactory;

    private final MqttProperties mqttConnectProperties;
    
    // 客户端缓存，避免重复创建相同 Client ID 的客户端
    private final Map<String, SimpleMqttClient> clientCache = new ConcurrentHashMap<>();

    public RealMqttPahoClientFactory(Mqtt5ClientFactory mqtt5ClientFactory, MqttProperties mqttConnectProperties) {
        super();
        this.mqtt5ClientFactory = mqtt5ClientFactory;
        this.mqttConnectProperties = mqttConnectProperties;
    }

    @SneakyThrows
    public SimpleMqttClient getClientInstance() {
        // 创建客户端
        String clientId = "simple:" + System.currentTimeMillis();
        return this.getClientInstance(clientId);
    }

    private final String mqttTag = IdUtil.simpleUUID();

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
            log.info("🔌 Creating new MQTT client: ClientId={}", id);
            MqttProperties properties = new MqttProperties();
            BeanUtil.copyProperties(mqttConnectProperties, properties);
            properties.setClientId(id);
            Mqtt5AsyncClient mqtt5AsyncClient = mqtt5ClientFactory.mqttClient(properties, null);
            return new SimpleMqttClient(mqtt5AsyncClient, properties);
        });
    }


    @SneakyThrows
    public SimpleMqttClient getClientInstance(String clientId) {
        // 使用缓存，避免重复创建
        return clientCache.computeIfAbsent(clientId, id -> {
            log.info("🔌 Creating new MQTT client: ClientId={}", id);
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

}
