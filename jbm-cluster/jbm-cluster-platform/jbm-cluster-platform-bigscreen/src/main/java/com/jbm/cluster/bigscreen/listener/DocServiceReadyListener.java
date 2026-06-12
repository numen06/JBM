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
 * 文档服务与 Minio 就绪监听器。
 * 三级门禁（Nacos 注册、doc 健康、Minio 探通）全部通过后才触发大屏加载。
 */
@Component
@Slf4j
public class DocServiceReadyListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final String DOC_SERVICE_NAME = "jbm-cluster-platform-doc";
    private static final String HEALTH_PATH = "/actuator/health";
    private static final String MINIO_HEALTH_PATH = "/health/minio";
    private static final int HTTP_TIMEOUT_MS = 3000;
    private static final long MAX_WAIT_TIME_MS = 5 * 60 * 1000L;
    private static final long CHECK_INTERVAL_MS = 2000L;

    @Resource
    private DiscoveryClient discoveryClient;

    @Autowired(required = false)
    private BigscreenViewService bigscreenViewService;

    private final AtomicBoolean loading = new AtomicBoolean(false);

    private volatile long applicationStartTime = 0;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        applicationStartTime = System.currentTimeMillis();
        log.info("开始监听文档服务[{}]与 Minio 状态，就绪后触发大屏加载", DOC_SERVICE_NAME);
        waitForStorageAndTrigger();
    }

    private void waitForStorageAndTrigger() {
        while (true) {
            long elapsed = System.currentTimeMillis() - applicationStartTime;
            if (elapsed > MAX_WAIT_TIME_MS) {
                log.warn("初始轮询超时（{}分钟），将由定时任务继续检查 Minio 就绪状态",
                        MAX_WAIT_TIME_MS / 60000);
                break;
            }
            tryLoadIfReady(false);
            if (!needsLoad()) {
                return;
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
        if (!isFullyReady()) {
            if (scheduled) {
                log.debug("⏳ Minio 未就绪，跳过加载");
            }
            return;
        }
        if (!loading.compareAndSet(false, true)) {
            return;
        }
        try {
            log.info("✅ Minio 已就绪，开始加载大屏");
            bigscreenViewService.loadAllBigscreens();
            log.info("✅ 所有大屏加载完成");
        } catch (Exception e) {
            log.error("触发大屏加载异常", e);
        } finally {
            loading.set(false);
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

    private boolean isFullyReady() {
        ServiceInstance instance = getDocInstance();
        if (ObjectUtil.isEmpty(instance)) {
            return false;
        }
        String baseUri = instance.getUri().toString();
        if (!isDocHealthUp(baseUri)) {
            log.debug("⏳ 文档服务 HTTP 未就绪: {}", baseUri);
            return false;
        }
        if (!isStorageReady(baseUri)) {
            log.debug("⏳ Minio 存储链路未就绪: {}", baseUri);
            return false;
        }
        return true;
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

    private boolean isDocHealthUp(String baseUri) {
        return probeHttp(baseUri + HEALTH_PATH, "\"status\":\"UP\"");
    }

    private boolean isStorageReady(String baseUri) {
        return probeHttp(baseUri + MINIO_HEALTH_PATH, "\"ready\":true");
    }

    private boolean probeHttp(String url, String successMarker) {
        try (HttpResponse response = HttpRequest.get(url).timeout(HTTP_TIMEOUT_MS).execute()) {
            if (!response.isOk()) {
                return false;
            }
            String body = response.body();
            return StrUtil.isNotBlank(body) && body.contains(successMarker);
        } catch (Exception e) {
            log.debug("探针请求失败 [{}]: {}", url, e.getMessage());
            return false;
        }
    }
}
