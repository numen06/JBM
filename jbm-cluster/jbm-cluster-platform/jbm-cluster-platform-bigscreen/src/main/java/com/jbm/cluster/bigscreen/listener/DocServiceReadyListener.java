package com.jbm.cluster.bigscreen.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.jbm.cluster.bigscreen.service.BigscreenViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文档服务就绪监听器。
 * 大屏只问 doc「文件服务是否就绪」（GET /health/file），不关心 doc 内部存储实现。
 */
@Component
@Slf4j
public class DocServiceReadyListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final String DOC_SERVICE_NAME = "jbm-cluster-platform-doc";
    private static final String DOC_FILE_READY_PATH = "/health/file";
    private static final String DOC_READY_MARKER = "\"ready\":true";
    private static final int HTTP_TIMEOUT_MS = 3000;
    private static final long MAX_WAIT_TIME_MS = 5 * 60 * 1000L;
    private static final long CHECK_INTERVAL_MS = 2000L;
    private static final long LOG_INTERVAL_MS = 30 * 1000L;

    @Resource
    private DiscoveryClient discoveryClient;

    @Autowired(required = false)
    private BigscreenViewService bigscreenViewService;

    private final AtomicBoolean loading = new AtomicBoolean(false);

    private volatile long applicationStartTime = 0;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        applicationStartTime = System.currentTimeMillis();
        log.info("开始监听文档服务[{}]，就绪后触发大屏加载", DOC_SERVICE_NAME);
        ThreadUtil.execute(this::waitForDocAndTrigger);
    }

    private void waitForDocAndTrigger() {
        long lastLogTime = 0;
        while (true) {
            long elapsed = System.currentTimeMillis() - applicationStartTime;
            if (elapsed > MAX_WAIT_TIME_MS) {
                log.warn("初始轮询超时（{}分钟），将由定时任务继续检查文档服务状态",
                        MAX_WAIT_TIME_MS / 60000);
                break;
            }
            tryLoadIfReady(false);
            if (!needsLoad()) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastLogTime >= LOG_INTERVAL_MS) {
                logWaitingReason();
                lastLogTime = now;
            }
            ThreadUtil.sleep(CHECK_INTERVAL_MS);
        }
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void scheduledCheckAndTrigger() {
        tryLoadIfReady(true);
    }

    private void tryLoadIfReady(boolean scheduled) {
        if (bigscreenViewService == null) {
            return;
        }
        if (!needsLoad()) {
            return;
        }
        if (!isDocReady()) {
            if (scheduled) {
                logWaitingReason();
            }
            return;
        }
        if (!loading.compareAndSet(false, true)) {
            return;
        }
        try {
            log.info("文档服务已就绪，开始加载大屏");
            bigscreenViewService.loadAllBigscreens();
            log.info("所有大屏加载完成");
        } catch (Exception e) {
            log.error("触发大屏加载异常", e);
        } finally {
            loading.set(false);
        }
    }

    private void logWaitingReason() {
        ServiceInstance instance = getDocInstance();
        if (ObjectUtil.isEmpty(instance)) {
            log.info("等待文档服务 [{}] 在 Nacos 注册", DOC_SERVICE_NAME);
            return;
        }
        log.info("等待文档服务具备文件下载能力: {}", instance.getUri());
    }

    private boolean needsLoad() {
        try {
            return bigscreenViewService.hasPendingLoad();
        } catch (Exception e) {
            log.error("检查待加载大屏异常", e);
            return true;
        }
    }

    private boolean isDocReady() {
        ServiceInstance instance = getDocInstance();
        if (ObjectUtil.isEmpty(instance)) {
            return false;
        }
        return isDocReadyForDownload(instance.getUri().toString());
    }

    private ServiceInstance getDocInstance() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(DOC_SERVICE_NAME);
            return CollUtil.getFirst(instances);
        } catch (Exception e) {
            log.debug("获取文档服务实例异常: {}", e.getMessage());
            return null;
        }
    }

    private boolean isDocReadyForDownload(String baseUri) {
        try (HttpResponse response = HttpRequest.get(baseUri + DOC_FILE_READY_PATH).timeout(HTTP_TIMEOUT_MS).execute()) {
            if (!response.isOk()) {
                return false;
            }
            String body = response.body();
            return StrUtil.isNotBlank(body) && body.contains(DOC_READY_MARKER);
        } catch (Exception e) {
            log.debug("文档文件服务探针失败 [{}]: {}", baseUri + DOC_FILE_READY_PATH, e.getMessage());
            return false;
        }
    }
}
