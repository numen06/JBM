package com.jbm.cluster.push.bevent;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wesley
 */
@Service
@Slf4j
public class EventStorageService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String EVENT_QUEUE_KEY = "jbm:bevent:%s";

    /**
     * @param url 可能是按照service分组也是可能指定的url
     * 存储待发送的 Webhook 任务（序列化为 JSON）
     */
    public void enqueueTask(String url, WebhookTask task) {
        try {
            String taskJson = JSON.toJSONString(task);
            // 防止中文乱码
            final String queueKey = String.format(EVENT_QUEUE_KEY, URLUtil.decode(url));

            redisTemplate.opsForList().rightPush(queueKey, taskJson);
            // 3天过期
            redisTemplate.expire(queueKey, Duration.ofDays(3));

            log.debug("📥 Task {} enqueued for service {}", task.getTaskId(), url);
        } catch (Exception e) {
            log.error("Failed to enqueue task {}", task.getTaskId(), e);
            throw new RuntimeException("Enqueue failed", e);
        }
    }

    /**
     * 获取待处理任务（反序列化）
     */
    public List<WebhookTask> getPendingTasks(String url, int count) {
        String queueKey = String.format(EVENT_QUEUE_KEY, url);
        List<String> taskJsons = redisTemplate.opsForList().range(queueKey, 0, count - 1);
        if (taskJsons == null || taskJsons.isEmpty()) {
            return Collections.emptyList();
        }
        return taskJsons.stream()
                .map(json -> JSON.parseObject(json, WebhookTask.class))
                .collect(Collectors.toList());
    }


    public void ackTask(String url) {
        ackTasks(url, 1);
    }
    /**
     * 确认发送成功，从队列移除
     */
    public void ackTasks(String url, int count) {
        if (count <= 0) {
            return;
        }
        String queueKey = String.format(EVENT_QUEUE_KEY, url);
        redisTemplate.execute((RedisCallback<Void>) conn -> {
            byte[] key = queueKey.getBytes(StandardCharsets.UTF_8);
            conn.lTrim(key, count, -1);
            return null;
        });
        log.debug("✅ Acked {} tasks for service {}", count, url);
    }
}