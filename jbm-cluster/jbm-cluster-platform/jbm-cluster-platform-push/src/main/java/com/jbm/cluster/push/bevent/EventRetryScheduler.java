package com.jbm.cluster.push.bevent;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.push.bevent.lis.ServiceOfflineEvent;
import com.jbm.cluster.push.bevent.lis.ServiceOnlineEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * @author wesley
 */
@Component
@Slf4j
@EnableScheduling
public class EventRetryScheduler {

    @Autowired
    private EventDeliveryService eventDeliveryService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 监控的服务列表（可配置化）
    private static final List<String> MONITORED_SERVICES = new ArrayList<>();

    @Scheduled(fixedDelay = 30000) // 每30秒兜底扫描
    public void retryAllPendingEvents() {
        for (String service : MONITORED_SERVICES) {
            eventDeliveryService.deliverPendingTasks(service);
        }
    }


    // ========== 监听服务上线 ==========
    @Async
    @EventListener
    public void handleServiceOnline(ServiceOnlineEvent event) {
        String serviceName = event.getServiceId();
        log.info("🔔 服务上线: {}, 触发积压任务投递", serviceName);
        eventDeliveryService.deliverPendingTasks(serviceName);
    }

    // ========== 监听服务下线（可选告警） ==========
    @Async
    @EventListener
    public void handleServiceOffline(ServiceOfflineEvent event) {
        String serviceName = event.getServiceId();
        log.warn("⚠️ 服务下线: {}, 存在积压任务风险", serviceName);
        // 可选：发送告警、记录监控指标等
    }



    @PostConstruct
    public void recoverOrphanTasksOnStartup() {
        log.info("🔍 启动时扫描所有 processing 队列，恢复孤儿任务...");

        String urlQueuePrefix = EventStorageService.EVENT_QUEUE_KEY_PREFIX+"*";
        // 获取所有 processing keys（按 URL 分片）
        Set<String> processingKeys = redisTemplate.keys(urlQueuePrefix);
        if (processingKeys.isEmpty()) {
            return;
        }
        for (String processingKey : processingKeys) {
            String taskUrl = StrUtil.removePrefixIgnoreCase(processingKey, EventStorageService.EVENT_QUEUE_KEY_PREFIX);
            eventDeliveryService.deliverPendingTasks(taskUrl);
        }

        log.info("✅ 孤儿任务恢复完成");
    }
}
