package com.jbm.cluster.push.bevent;

import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.common.basic.module.JbmRequestTemplate;
import com.jbm.cluster.push.bevent.lis.WebhookTaskEndEvent;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author wesley
 */
@Service
@Slf4j
public class EventDeliveryService {

    @Autowired
    private EventStorageService eventStorageService;

    @Autowired
    private JbmRequestTemplate jbmRequestTemplate;

    private static final int MAX_RETRY = 3;

    // 防止并发重复处理
    private final Set<String> deliveringServices = ConcurrentHashMap.newKeySet();


    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // ========== 核心投递逻辑 ==========
    @Async
    public void deliverPendingTasks(String url) {
        if (!deliveringServices.add(url)) {
            log.debug("🚫 服务 {} 正在处理中，跳过本次触发", url);
            return;
        }

        try {
            // 1. 获取待发送任务（最多10个）
            List<WebhookTask> tasks = eventStorageService.getPendingTasks(url, 10);
            if (tasks.isEmpty()) {
                return;
            }

            for (WebhookTask task : tasks) {
                try {
                    // 🚀 发送任务并获取实际尝试次数
                    int attemptCount = sendTaskWithRetry(task);

                    // 🆕 判断是否最终成功
                    boolean success = TaskStatus.SUCCESS.toString().equals(task.getStatus());

                    // 🎉 无论成功失败，发布任务结束事件
                    eventPublisher.publishEvent(WebhookTaskEndEvent.success(this, task));

                    // ✅ 如果成功，ACK 单个任务（从队列中移除）
                    if (success) {
                        // ⬅️ 假设你有这个方法
                        eventStorageService.ackTask(url);
                        log.debug("✅ 任务 {} 投递成功，尝试 {} 次，已ACK", task.getTaskId(), attemptCount);
                    } else {
                        log.warn("❌ 任务 {} 投递失败，尝试 {} 次，等待下次重试或进入死信", task.getTaskId(), attemptCount);
                        // ❗ 失败不ACK，下次还会被取出重试（直到超过 MAX_RETRY，在 sendTaskWithRetry 中已标记 FAILED）
                    }

                } catch (Exception e) {
                    log.error("🔥 处理任务 {} 时发生未预期异常", task.getTaskId(), e);
                    // 可选：发布失败事件 or 记录死信
                    try {
                        eventPublisher.publishEvent(WebhookTaskEndEvent.failed(this, task));
                    } catch (Exception ex) {
                        log.error("❌ 发布异常结束事件失败: {}", task.getTaskId(), ex);
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ 投递服务 {} 任务时发生异常", url, e);
        } finally {
            deliveringServices.remove(url);
        }
    }

    // ========== 带重试的发送逻辑 ==========
    /**
     * 发送任务并重试，返回实际尝试次数（从1开始计数）
     * @param task 任务对象
     * @return 实际尝试次数（最小为1，最大为 MAX_RETRY + 1）
     */
    private int sendTaskWithRetry(WebhookTask task) {
        int currentRetry = task.getRetryNumber() != null ? task.getRetryNumber() : 0;
        // 🆕 记录实际尝试次数
        int attemptCount = 0;

        while (currentRetry <= MAX_RETRY) {
            attemptCount++; // 🎯 每次进入循环，尝试次数+1

            if (currentRetry == MAX_RETRY) {
                // 💀 已达最大重试次数，本次不尝试，直接标记失败
                task.setStatus(TaskStatus.FAILED.toString());
                task.setErrorMsg("超过最大重试次数 " + MAX_RETRY);
                // 返回尝试次数（如 MAX_RETRY + 1）
                return attemptCount;
            }

            try {
                Response response = jbmRequestTemplate.request(
                        task.getTaskUrl(),
                        task.getTaskMethod(),
                        task.getRequest()
                );

                if (response.isSuccessful()) {
                    task.setStatus(TaskStatus.SUCCESS.toString());
                    task.setHttpStatus(response.code());
                    // 记录最终使用的重试编号
                    task.setRetryNumber(currentRetry);
                    // ✅ 成功，返回当前尝试次数
                    return attemptCount;
                } else {
                    throw new RuntimeException("HTTP " + response.code() + " " + response.message());
                }

            } catch (Exception e) {
                currentRetry++;
                task.setRetryNumber(currentRetry);
                task.setErrorMsg("第 " + currentRetry + " 次失败: " + e.getMessage());

                if (currentRetry < MAX_RETRY) {
                    long backoffMillis = (long) Math.pow(2, currentRetry - 1) * 1000;
                    try {
                        log.warn("⏳ 第 {} 次重试前等待 {}ms: {}", currentRetry, backoffMillis, task.getTaskId());
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.warn("⚠️ 重试等待被中断: {}", task.getTaskId());
                    }
                    // ➿ 继续下一次尝试
                } else {
                    // 💀 最后一次尝试也失败
                    task.setStatus(TaskStatus.FAILED.toString());
                    // 返回总尝试次数
                    return attemptCount;
                }
            }
        }
        // 理论上不会走到这里
        return attemptCount;
    }
}