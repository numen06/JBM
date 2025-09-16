package com.jbm.util.thread;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 内存版限流器：1分钟内超过60次访问则进入黑名单
 * @author wesley
 */
public class InMemoryBlacklistRateLimiter {

    // 存储每个用户的访问时间戳队列（毫秒），Key: 用户标识（如IP或key）
    private final Map<String, Deque<Long>> userTimestamps = new ConcurrentHashMap<>();

    // 时间窗口：1分钟 = 60,000 毫秒
    private final long windowTime;

    private final TimeUnit timeUnit;

    // 最大允许请求次数
    private final int maxRequests;

    public InMemoryBlacklistRateLimiter(long time, TimeUnit timeUnit,int maxRequests) {
        this.maxRequests = maxRequests;
        this.windowTime = timeUnit.toMillis(time);
        this.timeUnit = timeUnit;
    }

    /**
     * 检查用户是否在黑名单中（即1分钟内访问超过60次）
     * 如果是，则返回 true（被限流/进入黑名单）
     *
     * @param key 用户唯一标识（如 IP 地址、用户ID 等）
     * @return true 表示已被拉黑，false 表示正常
     */
    public boolean isBlacklisted(String key) {
        long currentTime = System.currentTimeMillis();

        // 获取该用户的时间戳队列，若不存在则创建
        Deque<Long> deque = userTimestamps.computeIfAbsent(key, k -> new LinkedList<>());

        // 清理过期请求：移除早于1分钟前的所有记录
        while (!deque.isEmpty() && deque.peekFirst() < currentTime - windowTime) {
            deque.removeFirst();
        }

        // 判断当前请求数是否超过60次
        if (deque.size() >= maxRequests) {
            // 已进入黑名单
            return true;
        }

        // 记录本次请求时间
        deque.addLast(currentTime);
        // 未被拉黑
        return false;
    }

    /**
     * 手动将某个用户加入黑名单（可选功能）
     */
    public void addToBlacklist(String key) {
        // 强制清空并塞满60个时间戳，使其判定为拉黑
        Deque<Long> deque = userTimestamps.computeIfAbsent(key, k -> new LinkedList<>());
        deque.clear();
        long now = System.currentTimeMillis();
        for (int i = 0; i < maxRequests; i++) {
            deque.addLast(now);
        }
    }

    /**
     * 从黑名单移除（重置该用户访问记录）
     */
    public void removeFromBlacklist(String key) {
        userTimestamps.remove(key);
    }

    /**
     * 获取用户当前在窗口内的访问次数（用于监控）
     */
    public int getRequestCount(String key) {
        long currentTime = System.currentTimeMillis();
        Deque<Long> deque = userTimestamps.get(key);
        if (deque == null) {
            return 0;
        }

        // 清理过期数据并返回当前有效请求数
        while (!deque.isEmpty() && deque.peekFirst() < currentTime - maxRequests) {
            deque.removeFirst();
        }
        return deque.size();
    }
}