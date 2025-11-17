package com.jbm.cluster.push.bevent.lis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


import org.springframework.context.ApplicationEventPublisher;

/**
 * 服务实例变更发布器
 * 监控服务的上线和下线，发布相应的事件
 * @author wesley
 */
@Component
@Slf4j
public class ServiceInstanceChangePublisher {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // 缓存每个服务当前的实例ID集合
    private final Map<String, Set<String>> currentInstances = new ConcurrentHashMap<>();
    
    // 标记是否已初始化
    private volatile boolean initialized = false;
    
    // 初始化尝试次数
    private int initAttempts = 0;
    private static final int MAX_INIT_ATTEMPTS = 3;

    /**
     * 应用启动完成后，初始化所有在线服务
     * 注意：这是一个"尽力而为"的初始化，即使失败也不影响后续的心跳检测
     * 心跳检测会自动补充遗漏的服务
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (initialized) {
            return;
        }
        
        initAttempts++;
        
        try {
            log.info("🚀 初始化服务实例状态... (第 {} 次尝试)", initAttempts);
            List<String> allServices = discoveryClient.getServices();
            
            if (allServices.isEmpty()) {
                log.warn("⚠️ DiscoveryClient 返回空服务列表，可能还未准备好");
                if (initAttempts < MAX_INIT_ATTEMPTS) {
                    log.info("⏳ 将依赖后续心跳检测来发现服务");
                }
                // 即使没有服务，也标记为已初始化，避免阻塞
                initialized = true;
                return;
            }
            
            int onlineServiceCount = 0;
            for (String serviceId : allServices) {
                try {
                    List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
                    if (!instances.isEmpty()) {
                        Set<String> instanceIds = instances.stream()
                                .map(ServiceInstance::getInstanceId)
                                .collect(Collectors.toSet());
                        currentInstances.put(serviceId, instanceIds);
                        
                        // 发布服务在线事件
                        eventPublisher.publishEvent(new ServiceOnlineEvent(this, serviceId));
                        log.info("✅ 初始化服务 {} 状态: {} 个实例在线", serviceId, instances.size());
                        onlineServiceCount++;
                    } else {
                        log.debug("📋 服务 {} 当前无实例在线", serviceId);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ 初始化服务 {} 状态失败: {}", serviceId, e.getMessage());
                }
            }
            
            initialized = true;
            log.info("✅ 服务实例状态初始化完成，共 {} 个服务，{} 个在线", 
                    allServices.size(), onlineServiceCount);
        } catch (Exception e) {
            log.error("❌ 初始化服务实例状态失败 (第 {} 次尝试)", initAttempts, e);
            // 即使失败也标记为已初始化，依赖后续心跳检测
            initialized = true;
            log.info("⏳ 初始化失败，将依赖后续心跳检测来发现服务");
        }
    }

    /**
     * 监听心跳事件，检测服务实例变化
     * 这个方法会自动发现初始化时遗漏的服务
     */
    @EventListener
    public void onHeartbeat(HeartbeatEvent event) {
        log.debug("💓 Heartbeat received, scanning for service changes...");

        try {
            List<String> allServices = discoveryClient.getServices();

            for (String serviceId : allServices) {
                try {
                    List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
                    Set<String> newInstanceIds = instances.stream()
                            .map(ServiceInstance::getInstanceId)
                            .collect(Collectors.toSet());

                    Set<String> oldInstanceIds = currentInstances.getOrDefault(serviceId, new HashSet<>());
                    
                    boolean isNewService = !currentInstances.containsKey(serviceId);

                    // 检测上线实例
                    Set<String> onlineInstances = new HashSet<>(newInstanceIds);
                    onlineInstances.removeAll(oldInstanceIds);
                    
                    if (!onlineInstances.isEmpty()) {
                        if (isNewService) {
                            // 新发现的服务（初始化时可能遗漏的）
                            log.info("🆕 发现新服务 {} 上线，实例数: {}", serviceId, newInstanceIds.size());
                        } else {
                            // 已知服务的新实例上线
                            log.info("🎉 服务 {} 新增实例: {}", serviceId, onlineInstances);
                        }
                        eventPublisher.publishEvent(new ServiceOnlineEvent(this, serviceId));
                    }

                    // 检测下线实例
                    Set<String> offlineInstances = new HashSet<>(oldInstanceIds);
                    offlineInstances.removeAll(newInstanceIds);
                    if (!offlineInstances.isEmpty()) {
                        log.warn("💔 服务 {} 下线实例: {}", serviceId, offlineInstances);
                        
                        // 如果服务的所有实例都下线了，发布下线事件
                        if (newInstanceIds.isEmpty()) {
                            log.warn("⚠️ 服务 {} 所有实例已下线", serviceId);
                            eventPublisher.publishEvent(new ServiceOfflineEvent(this, serviceId));
                        }
                    }

                    // 更新缓存（即使是空集合也要更新，以便跟踪服务状态）
                    if (!newInstanceIds.isEmpty()) {
                        currentInstances.put(serviceId, new HashSet<>(newInstanceIds));
                    } else if (!oldInstanceIds.isEmpty()) {
                        // 服务从有实例变为无实例，仍然保留在缓存中，但设为空集合
                        currentInstances.put(serviceId, new HashSet<>());
                    }
                } catch (Exception e) {
                    log.warn("⚠️ 处理服务 {} 的心跳检测失败: {}", serviceId, e.getMessage());
                }
            }
            
            // 检测已从注册中心移除的服务
            Set<String> removedServices = new HashSet<>(currentInstances.keySet());
            removedServices.removeAll(allServices);
            for (String removedService : removedServices) {
                if (!currentInstances.get(removedService).isEmpty()) {
                    log.warn("🗑️ 服务 {} 已从注册中心移除", removedService);
                    eventPublisher.publishEvent(new ServiceOfflineEvent(this, removedService));
                }
                currentInstances.remove(removedService);
            }
        } catch (Exception e) {
            log.error("❌ 心跳检测处理失败", e);
        }
    }
    
    /**
     * 获取当前所有在线服务
     * @return 在线服务列表
     */
    public List<String> getOnlineServices() {
        return new ArrayList<>(currentInstances.keySet());
    }
    
    /**
     * 检查指定服务是否在线
     * @param serviceId 服务ID
     * @return 如果服务在线返回true，否则返回false
     */
    public boolean isServiceOnline(String serviceId) {
        Set<String> instances = currentInstances.get(serviceId);
        return instances != null && !instances.isEmpty();
    }
    
    /**
     * 获取服务的实例数量
     * @param serviceId 服务ID
     * @return 实例数量
     */
    public int getInstanceCount(String serviceId) {
        Set<String> instances = currentInstances.get(serviceId);
        return instances != null ? instances.size() : 0;
    }
}