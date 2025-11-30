package com.jbm.cluster.push.bevent;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.common.basic.module.JbmRequestTemplate;
import com.jbm.cluster.push.bevent.lis.WebhookTaskEndEvent;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;
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

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private static final int MAX_RETRY = 3;

    // 防止并发重复处理
    private final Set<String> deliveringServices = ConcurrentHashMap.newKeySet();


    @Async
    public void enqueueAndTrigger(WebhookTask task) {
        final String url = task.getTaskUrl();
        final String targetKey = ServiceNameExtractor.getEnqueueName(url);
        eventStorageService.enqueueTask(targetKey, task);
        this.deliverPendingTasks(targetKey);
    }

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

                    boolean success = TaskStatus.SUCCESS.toString().equals(task.getStatus());

                    // 🎉 无论成功失败，发布任务结束事件
                    eventPublisher.publishEvent(new WebhookTaskEndEvent(this, task));
                    // ✅ 如果成功，ACK 单个任务（从队列中移除）
                    if (success) {
                        log.debug("✅ 任务 {} 投递成功，尝试 {} 次，已ACK", task.getTaskId(), attemptCount);
                    }
                    eventStorageService.ackTask(url);
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
     *
     * @param task 任务对象
     * @return 实际尝试次数（最小为1，最大为 MAX_RETRY + 1）
     */
    private int sendTaskWithRetry(WebhookTask task) {
        int currentRetry = task.getRetryNumber() != null ? task.getRetryNumber() : 0;
        int attemptCount = 0;
        while (true) {
            attemptCount++;
            try {
                Response response = jbmRequestTemplate.request(
                        task.getTaskUrl(),
                        task.getTaskMethod(),
                        task.getRequest()
                );
                //只要不是404都是成功
                if (response.code() != 404) {
                    task.setStatus(TaskStatus.SUCCESS.toString());
                    task.setHttpStatus(response.code());
                    if (response.body() != null) {
                        task.setResponse(response.body().string());
                    }
                    task.setRetryNumber(currentRetry);
                    log.info("✅ 任务 {} 第 {} 次尝试成功", task.getTaskId(), attemptCount);
                    return attemptCount;
                } else {
                    throw new RuntimeException("HTTP " + response.code() + " " + response.message());
                }
            } catch (Exception e) {
                // 💀 最后一次尝试（第 MAX_RETRY + 1 次）失败 → 标记失败
                if (attemptCount > MAX_RETRY) {
                    // 🎯 关键：用 attemptCount 判断是否超限
                    // 更新重试次数为最终尝试次数
                    task.setRetryNumber(currentRetry);
                    task.setStatus(TaskStatus.FAILED.toString());
                    String finalErrorMsg = "已达最大尝试次数 " + (MAX_RETRY + 1) + "，最终错误: " + e.getMessage();
                    task.setErrorMsg(finalErrorMsg);
                    this.buildErrorMsg(task, StrUtil.format("第{}次失败: {}", currentRetry + 1, e.getMessage()));
                    log.error("💀 任务 {} 经过 {} 次尝试后最终失败，重试次数: {}", task.getTaskId(), attemptCount, task.getRetryNumber());
                    return attemptCount;
                }

                // 🔄 否则，准备重试
                // 更新重试次数（每次失败后递增）
                currentRetry++;
                task.setRetryNumber(currentRetry);
                String msg = StrUtil.format("第{}次失败: {}", currentRetry, e.getMessage());
                this.buildErrorMsg(task, msg);
                // 设置状态为 RETRYING，表示正在重试中
                task.setStatus(TaskStatus.RETRYING.toString());
                log.warn("⏳ 任务 {} 第 {} 次尝试失败，重试次数: {}，准备重试", 
                        task.getTaskId(), attemptCount, task.getRetryNumber());

                long backoffMillis = (long) Math.pow(2, attemptCount - 1) * 1000;
                log.warn("⏳ 第 {} 次重试前等待 {}ms: {}", attemptCount, backoffMillis, task.getTaskId());
                ThreadUtil.safeSleep(backoffMillis);
                // ➿ 继续下一次尝试
            }
        }
    }

    private void buildErrorMsg(WebhookTask webhookTask, String... errorMsg) {
        String format = "{} : {}";
        StringBuilder sb = new StringBuilder();
        sb.append(StrUtil.emptyIfNull(webhookTask.getErrorMsg()));
        for (String s : errorMsg) {
            String msg = StrUtil.format(format, DateUtil.now(), StrUtil.emptyToDefault(s, "无"));
            sb.append("\r\n").append(msg);
        }
        webhookTask.setErrorMsg(StrUtil.trimToEmpty(sb.toString()));
    }
}