package com.jbm.cluster.push.service;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.push.bevent.lis.ServiceOfflineEvent;
import com.jbm.cluster.push.bevent.lis.ServiceOnlineEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Webhook配置选择策略类
 * 负责从一组配置中选择最优的配置进行发送
 * 考虑服务运行状态，优先选择在线服务
 */
@Component
@Slf4j
public class WebhookConfigSelectionStrategy {

    // 存储每个分组最后成功发送的配置ID
    private final Map<String, String> lastSuccessConfigMap = new ConcurrentHashMap<>();
    
    // 存储在线服务列表
    private final Set<String> onlineServices = ConcurrentHashMap.newKeySet();
    
    // 存储每个服务的失败次数（用于降级）
    private final Map<String, Integer> serviceFailureCount = new ConcurrentHashMap<>();
    
    // 最大失败次数阈值
    private static final int MAX_FAILURE_THRESHOLD = 3;

    /**
     * 监听服务上线事件
     */
    @EventListener
    public void onServiceOnline(ServiceOnlineEvent event) {
        String serviceId = event.getServiceId();
        onlineServices.add(serviceId);
        // 服务恢复上线时，清空失败计数
        serviceFailureCount.remove(serviceId);
        log.info("🟢 服务 {} 已上线，当前在线服务数: {}", serviceId, onlineServices.size());
    }
    
    /**
     * 监听服务下线事件
     */
    @EventListener
    public void onServiceOffline(ServiceOfflineEvent event) {
        String serviceId = event.getServiceId();
        onlineServices.remove(serviceId);
        log.warn("🔴 服务 {} 已下线，当前在线服务数: {}", serviceId, onlineServices.size());
    }

    /**
     * 从分组配置中选择一个最优配置
     * 策略优先级：
     * 1. 优先选择在线且上次成功的配置
     * 2. 其次选择任意在线的配置
     * 3. 最后选择失败次数较少的配置
     *
     * @param group       配置分组名称
     * @param webhookEventConfigs 分组内的配置列表
     * @return 选中的配置，如果没有可用配置则返回null
     */
    public WebhookEventConfig selectConfig(String group, List<WebhookEventConfig> webhookEventConfigs) {
        if (webhookEventConfigs == null || webhookEventConfigs.isEmpty()) {
            return null;
        }
        
        // 过滤启用的配置
        List<WebhookEventConfig> enabledConfigs = webhookEventConfigs.stream()
                .filter(config -> config.getEnable() != null && config.getEnable())
                .collect(Collectors.toList());
        
        if (enabledConfigs.isEmpty()) {
            log.warn("⚠️ 分组 {} 没有启用的配置", group);
            return null;
        }
        
        // 策略1: 优先选择在线且上次成功的配置
        WebhookEventConfig priorityConfig = getPriorityOnlineConfig(group, enabledConfigs);
        if (priorityConfig != null) {
            log.debug("✅ 选择上次成功的在线配置: {} (服务: {})", 
                    priorityConfig.getEventId(), priorityConfig.getServiceName());
            return priorityConfig;
        }
        
        // 策略2: 选择任意在线且健康的配置
        WebhookEventConfig onlineConfig = getHealthyOnlineConfig(enabledConfigs);
        if (onlineConfig != null) {
            log.debug("🟢 选择在线健康配置: {} (服务: {})", 
                    onlineConfig.getEventId(), onlineConfig.getServiceName());
            return onlineConfig;
        }
        
        // 策略3: 选择失败次数最少的配置
        WebhookEventConfig leastFailedConfig = getLeastFailedConfig(enabledConfigs);
        if (leastFailedConfig != null) {
            log.debug("⚡ 选择失败次数最少的配置: {} (服务: {})", 
                    leastFailedConfig.getEventId(), leastFailedConfig.getServiceName());
            return leastFailedConfig;
        }
        
        // 兜底: 返回第一个启用的配置
        log.warn("⚠️ 所有策略均未匹配，返回第一个启用配置");
        return enabledConfigs.get(0);
    }

    /**
     * 处理配置发送成功的情况
     *
     * @param group 配置分组名称
     * @param config 发送成功的配置
     */
    public void handleSuccess(String group, WebhookEventConfig config) {
        lastSuccessConfigMap.put(group, config.getEventId());
        // 成功时清空该服务的失败计数
        if (StrUtil.isNotBlank(config.getServiceName())) {
            serviceFailureCount.remove(config.getServiceName());
            log.debug("✅ 配置 {} (服务: {}) 发送成功，清空失败计数", 
                    config.getEventId(), config.getServiceName());
        }
    }

    /**
     * 处理配置发送失败的情况
     *
     * @param group 配置分组名称
     * @param config 发送失败的配置
     */
    public void handleFailure(String group, WebhookEventConfig config) {
        String lastSuccessConfigId = lastSuccessConfigMap.get(group);
        if (StrUtil.isNotBlank(lastSuccessConfigId) && StrUtil.equals(lastSuccessConfigId, config.getEventId())) {
            lastSuccessConfigMap.remove(group);
        }
        
        // 增加失败计数
        if (StrUtil.isNotBlank(config.getServiceName())) {
            serviceFailureCount.merge(config.getServiceName(), 1, Integer::sum);
            int count = serviceFailureCount.get(config.getServiceName());
            log.warn("❌ 配置 {} (服务: {}) 发送失败，当前失败计数: {}", 
                    config.getEventId(), config.getServiceName(), count);
        }
    }

    /**
     * 获取优先的在线配置（上次成功 + 在线）
     *
     * @param group 配置分组名称
     * @param webhookEventConfigs 分组内的配置列表
     * @return 优先配置，如果不存在或不可用则返回null
     */
    private WebhookEventConfig getPriorityOnlineConfig(String group, List<WebhookEventConfig> webhookEventConfigs) {
        String lastSuccessConfigId = lastSuccessConfigMap.get(group);
        if (StrUtil.isBlank(lastSuccessConfigId)) {
            return null;
        }

        return webhookEventConfigs.stream()
                .filter(config -> StrUtil.equals(config.getEventId(), lastSuccessConfigId))
                .filter(this::isServiceOnline)
                .filter(this::isServiceHealthy)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取健康的在线配置
     *
     * @param webhookEventConfigs 配置列表
     * @return 在线且健康的配置，如果不存在则返回null
     */
    private WebhookEventConfig getHealthyOnlineConfig(List<WebhookEventConfig> webhookEventConfigs) {
        return webhookEventConfigs.stream()
                .filter(this::isServiceOnline)
                .filter(this::isServiceHealthy)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取失败次数最少的配置
     *
     * @param webhookEventConfigs 配置列表
     * @return 失败次数最少的配置
     */
    private WebhookEventConfig getLeastFailedConfig(List<WebhookEventConfig> webhookEventConfigs) {
        return webhookEventConfigs.stream()
                .min((c1, c2) -> {
                    int count1 = getFailureCount(c1);
                    int count2 = getFailureCount(c2);
                    return Integer.compare(count1, count2);
                })
                .orElse(null);
    }
    
    /**
     * 判断服务是否在线
     *
     * @param config 配置
     * @return 如果服务在线返回true，否则返回false
     */
    private boolean isServiceOnline(WebhookEventConfig config) {
        if (StrUtil.isBlank(config.getServiceName())) {
            // 如果没有配置服务名，认为是外部服务，始终认为在线
            return true;
        }
        return onlineServices.contains(config.getServiceName());
    }
    
    /**
     * 判断服务是否健康（失败次数未超过阈值）
     *
     * @param config 配置
     * @return 如果服务健康返回true，否则返回false
     */
    private boolean isServiceHealthy(WebhookEventConfig config) {
        if (StrUtil.isBlank(config.getServiceName())) {
            return true;
        }
        int failureCount = serviceFailureCount.getOrDefault(config.getServiceName(), 0);
        return failureCount < MAX_FAILURE_THRESHOLD;
    }
    
    /**
     * 获取服务的失败次数
     *
     * @param config 配置
     * @return 失败次数
     */
    private int getFailureCount(WebhookEventConfig config) {
        if (StrUtil.isBlank(config.getServiceName())) {
            return 0;
        }
        return serviceFailureCount.getOrDefault(config.getServiceName(), 0);
    }
    
    /**
     * 获取当前在线服务数量
     *
     * @return 在线服务数量
     */
    public int getOnlineServiceCount() {
        return onlineServices.size();
    }
    
    /**
     * 检查指定服务是否在线
     *
     * @param serviceName 服务名称
     * @return 如果服务在线返回true，否则返回false
     */
    public boolean isServiceOnline(String serviceName) {
        if (StrUtil.isBlank(serviceName)) {
            return false;
        }
        return onlineServices.contains(serviceName);
    }
}