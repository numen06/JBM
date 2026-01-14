package com.jbm.cluster.bigscreen.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.bigscreen.service.BigscreenViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文档服务就绪监听器
 * 等待文档服务启动就绪后再触发大屏加载
 *
 * @author wesley.zhang
 */
@Component
@Slf4j
public class DocServiceReadyListener implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private DiscoveryClient discoveryClient;

    @Autowired(required = false)
    private BigscreenViewService bigscreenViewService;

    /**
     * 目标服务名称
     */
    private static final String DOC_SERVICE_NAME = "jbm-cluster-platform-doc";

    /**
     * 是否已经触发过加载
     */
    private final AtomicBoolean hasTriggered = new AtomicBoolean(false);

    /**
     * 最大等待时间（毫秒），默认5分钟
     */
    private static final long MAX_WAIT_TIME_MS = 5 * 60 * 1000L;

    /**
     * 检查间隔（毫秒）
     */
    private static final long CHECK_INTERVAL_MS = 2000L;

    /**
     * 应用启动时间
     */
    private volatile long applicationStartTime = 0;

    @Override
    @Async
    public void onApplicationEvent(ApplicationReadyEvent event) {
        applicationStartTime = System.currentTimeMillis();
        log.info("开始监听文档服务[{}]状态，等待服务就绪后触发大屏加载", DOC_SERVICE_NAME);
        
        // 异步等待服务就绪
        waitForDocServiceAndTrigger();
    }

    /**
     * 等待文档服务就绪并触发大屏加载
     */
    private void waitForDocServiceAndTrigger() {
        while (!hasTriggered.get()) {
            try {
                // 检查是否超时
                long elapsed = System.currentTimeMillis() - applicationStartTime;
                if (elapsed > MAX_WAIT_TIME_MS) {
                    log.error("等待文档服务[{}]超时（{}分钟），停止等待", DOC_SERVICE_NAME, MAX_WAIT_TIME_MS / 60000);
                    break;
                }

                // 检查服务是否就绪
                if (isDocServiceReady()) {
                    if (hasTriggered.compareAndSet(false, true)) {
                        log.info("✅ 文档服务[{}]已就绪，开始触发大屏加载", DOC_SERVICE_NAME);
                        triggerBigscreenLoad();
                        return;
                    }
                } else {
                    log.debug("⏳ 文档服务[{}]未就绪，等待{}ms后继续检查...", DOC_SERVICE_NAME, CHECK_INTERVAL_MS);
                }

                // 等待后继续检查
                ThreadUtil.sleep(CHECK_INTERVAL_MS);
            } catch (Exception e) {
                log.error("等待文档服务就绪过程中发生异常", e);
                ThreadUtil.sleep(CHECK_INTERVAL_MS);
            }
        }
    }

    /**
     * 定时检查并触发（兜底机制，每30秒检查一次）
     * 用于处理异步等待可能遗漏的情况
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void scheduledCheckAndTrigger() {
        if (hasTriggered.get()) {
            return;
        }

        if (isDocServiceReady()) {
            if (hasTriggered.compareAndSet(false, true)) {
                log.info("✅ [定时检查]文档服务[{}]已就绪，开始触发大屏加载", DOC_SERVICE_NAME);
                triggerBigscreenLoad();
            }
        }
    }

    /**
     * 检查文档服务是否就绪
     *
     * @return true-就绪，false-未就绪
     */
    private boolean isDocServiceReady() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(DOC_SERVICE_NAME);
            ServiceInstance instance = CollUtil.getFirst(instances);
            return ObjectUtil.isNotEmpty(instance);
        } catch (Exception e) {
            log.debug("检查文档服务状态异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 触发大屏加载
     */
    private void triggerBigscreenLoad() {
        if (bigscreenViewService == null) {
            log.warn("BigscreenViewService未注入，无法触发大屏加载");
            return;
        }

        try {
            log.info("开始加载所有大屏...");
            bigscreenViewService.loadAllBigscreens();
            log.info("✅ 所有大屏加载完成");
        } catch (Exception e) {
            log.error("触发大屏加载异常", e);
        }
    }
}
