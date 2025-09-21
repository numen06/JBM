package com.jbm.cluster.push.bevent.lis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


import org.springframework.context.ApplicationEventPublisher;

/**
 * @author wesley
 */
@Component
@Slf4j
public class ServiceInstanceChangePublisher {

    @Autowired
    private org.springframework.cloud.client.discovery.DiscoveryClient discoveryClient;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // 缓存每个服务当前的实例ID集合
    private final Map<String, Set<String>> currentInstances = new ConcurrentHashMap<>();

    @EventListener
    public void onHeartbeat(HeartbeatEvent event) {
        log.debug("💓 Heartbeat received, scanning for service changes...");

        List<String> allServices = discoveryClient.getServices();

        for (String serviceId : allServices) {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            Set<String> newInstanceIds = instances.stream()
                    .map(ServiceInstance::getInstanceId)
                    .collect(Collectors.toSet());

            Set<String> oldInstanceIds = currentInstances.getOrDefault(serviceId, new HashSet<>());

            // 检测上线实例
            Set<String> onlineInstances = new HashSet<>(newInstanceIds);
            onlineInstances.removeAll(oldInstanceIds);
            if (!onlineInstances.isEmpty()) {
                log.info("🎉 Service {} 上线实例: {}", serviceId, onlineInstances);
                eventPublisher.publishEvent(new ServiceOnlineEvent(this, serviceId));
            }

            // 检测下线实例
            Set<String> offlineInstances = new HashSet<>(oldInstanceIds);
            offlineInstances.removeAll(newInstanceIds);
            if (!offlineInstances.isEmpty()) {
                log.warn("💔 Service {} 下线实例: {}", serviceId, offlineInstances);
                eventPublisher.publishEvent(new ServiceOfflineEvent(this, serviceId));
            }

            // 更新缓存
            currentInstances.put(serviceId, new HashSet<>(newInstanceIds));
        }
    }
}