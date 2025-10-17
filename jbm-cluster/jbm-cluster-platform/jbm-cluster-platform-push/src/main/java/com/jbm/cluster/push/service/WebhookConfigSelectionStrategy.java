package com.jbm.cluster.push.service;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Webhook配置选择策略类
 * 负责从一组配置中选择最优的配置进行发送
 */
@Component
@Slf4j
public class WebhookConfigSelectionStrategy {

    // 存储每个分组最后成功发送的配置ID
    private final Map<String, String> lastSuccessConfigMap = new ConcurrentHashMap<>();

    /**
     * 从分组配置中选择一个最优配置
     *
     * @param group       配置分组名称
     * @param webhookEventConfigs 分组内的配置列表
     * @return 选中的配置，如果没有可用配置则返回null
     */
    public WebhookEventConfig selectConfig(String group, List<WebhookEventConfig> webhookEventConfigs) {
        // 先尝试使用上次成功的配置
        WebhookEventConfig priorityConfig = getPriorityConfig(group, webhookEventConfigs);
        
        if (priorityConfig != null) {
            return priorityConfig;
        }
        
        // 如果没有上次成功的配置或者上次成功的配置不可用，则返回列表中的第一个作为候选
        if (!webhookEventConfigs.isEmpty()) {
            return webhookEventConfigs.get(0);
        }
        
        return null;
    }

    /**
     * 处理配置发送成功的情况
     *
     * @param group 配置分组名称
     * @param config 发送成功的配置
     */
    public void handleSuccess(String group, WebhookEventConfig config) {
        lastSuccessConfigMap.put(group, config.getEventId());
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
    }

    /**
     * 获取指定分组的优先配置
     *
     * @param group 配置分组名称
     * @param webhookEventConfigs 分组内的配置列表
     * @return 优先配置，如果不存在或不可用则返回null
     */
    private WebhookEventConfig getPriorityConfig(String group, List<WebhookEventConfig> webhookEventConfigs) {
        String lastSuccessConfigId = lastSuccessConfigMap.get(group);
        if (StrUtil.isBlank(lastSuccessConfigId)) {
            return null;
        }

        return webhookEventConfigs.stream()
                .filter(config -> StrUtil.equals(config.getEventId(), lastSuccessConfigId))
                .findFirst()
                .orElse(null);
    }
}