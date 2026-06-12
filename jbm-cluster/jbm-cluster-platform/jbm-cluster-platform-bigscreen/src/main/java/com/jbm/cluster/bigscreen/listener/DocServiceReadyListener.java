package com.jbm.cluster.bigscreen.listener;

import cn.hutool.core.thread.ThreadUtil;
import com.jbm.cluster.api.service.feign.RemoteFileService;
import com.jbm.cluster.bigscreen.service.BigscreenViewService;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文档服务就绪监听器。
 * 通过 Feign 调用 doc，能调通即说明 Nacos 注册且路由可达；仅再确认文件服务业务就绪后触发加载。
 */
@Component
@Slf4j
public class DocServiceReadyListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final long MAX_WAIT_TIME_MS = 5 * 60 * 1000L;
    private static final long CHECK_INTERVAL_MS = 2000L;
    private static final long LOG_INTERVAL_MS = 30 * 1000L;

    @Autowired(required = false)
    private RemoteFileService remoteFileService;

    @Autowired(required = false)
    private BigscreenViewService bigscreenViewService;

    private final AtomicBoolean loading = new AtomicBoolean(false);

    private volatile long applicationStartTime = 0;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        applicationStartTime = System.currentTimeMillis();
        log.info("开始通过 Feign 监听文档服务[{}]，文件就绪后触发大屏加载", JbmClusterConstants.DOC_SERVER);
        ThreadUtil.execute(this::waitForDocAndTrigger);
    }

    private void waitForDocAndTrigger() {
        long lastLogTime = 0;
        while (true) {
            long elapsed = System.currentTimeMillis() - applicationStartTime;
            if (elapsed > MAX_WAIT_TIME_MS) {
                log.warn("初始轮询超时（{}分钟），将由定时任务继续尝试", MAX_WAIT_TIME_MS / 60000);
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
        if (bigscreenViewService == null || remoteFileService == null) {
            return;
        }
        if (!needsLoad()) {
            return;
        }
        if (!isDocFileServiceReady()) {
            if (scheduled) {
                logWaitingReason();
            }
            return;
        }
        if (!loading.compareAndSet(false, true)) {
            return;
        }
        try {
            log.info("文档文件服务已就绪，开始加载大屏");
            bigscreenViewService.loadAllBigscreens();
            log.info("所有大屏加载完成");
        } catch (Exception e) {
            log.error("触发大屏加载异常", e);
        } finally {
            loading.set(false);
        }
    }

    private void logWaitingReason() {
        try {
            Map<String, Object> health = remoteFileService.fileHealth();
            if (health != null && Boolean.TRUE.equals(health.get("ready"))) {
                return;
            }
            log.info("文档服务 [{}] 已可达，等待文件服务就绪", JbmClusterConstants.DOC_SERVER);
        } catch (FeignException e) {
            log.info("等待文档服务 [{}] 可通过 Feign 访问", JbmClusterConstants.DOC_SERVER);
        } catch (Exception e) {
            log.info("等待文档服务 [{}] 可通过 Feign 访问: {}", JbmClusterConstants.DOC_SERVER, e.getMessage());
        }
    }

    private boolean needsLoad() {
        try {
            return bigscreenViewService.hasPendingLoad();
        } catch (Exception e) {
            log.error("检查待加载大屏异常", e);
            return true;
        }
    }

    private boolean isDocFileServiceReady() {
        try {
            Map<String, Object> health = remoteFileService.fileHealth();
            return health != null && Boolean.TRUE.equals(health.get("ready"));
        } catch (Exception e) {
            log.debug("Feign 调用文档服务未就绪: {}", e.getMessage());
            return false;
        }
    }
}
