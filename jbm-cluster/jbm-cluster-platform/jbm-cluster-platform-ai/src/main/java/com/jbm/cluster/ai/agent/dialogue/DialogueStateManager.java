package com.jbm.cluster.ai.agent.dialogue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话状态管理器
 * 
 * 管理所有活跃的对话状态，提供线程安全的 CRUD 操作
 * 
 * @author wesley
 */
@Slf4j
@Component
public class DialogueStateManager {
    
    /**
     * 存储所有活跃的对话状态
     * Key: sessionId
     * Value: DialogueState
     */
    private final Map<String, DialogueState> activeStates = new ConcurrentHashMap<>();
    
    /**
     * 默认会话超时时间（24小时）
     */
    private static final long DEFAULT_TIMEOUT_MS = 24 * 60 * 60 * 1000L;
    
    /**
     * 创建新的对话状态
     * 
     * @param state 对话状态
     */
    public void createState(DialogueState state) {
        if (state == null || state.getSessionId() == null) {
            log.warn("⚠️  [对话状态] 无效的状态对象，无法创建");
            return;
        }
        
        activeStates.put(state.getSessionId(), state);
        log.info("📝 [对话状态] 创建新状态: sessionId={}, api={} {}", 
                state.getSessionId(), 
                state.getSelectedApi() != null ? state.getSelectedApi().getMethod() : "?",
                state.getSelectedApi() != null ? state.getSelectedApi().getPath() : "?");
    }
    
    /**
     * 获取对话状态
     * 
     * @param sessionId 会话ID
     * @return 对话状态，不存在则返回 null
     */
    public DialogueState getState(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        return activeStates.get(sessionId);
    }
    
    /**
     * 更新对话状态
     * 
     * @param state 对话状态
     */
    public void updateState(DialogueState state) {
        if (state == null || state.getSessionId() == null) {
            log.warn("⚠️  [对话状态] 无效的状态对象，无法更新");
            return;
        }
        
        activeStates.put(state.getSessionId(), state);
        log.debug("🔄 [对话状态] 更新状态: sessionId={}, 缺失参数={}, 已收集={}", 
                state.getSessionId(), 
                state.getMissingRequiredParameters().size(),
                state.getCollectedParameters().size());
    }
    
    /**
     * 删除对话状态
     * 
     * @param sessionId 会话ID
     */
    public void removeState(String sessionId) {
        if (sessionId == null) {
            return;
        }
        
        DialogueState removed = activeStates.remove(sessionId);
        if (removed != null) {
            log.info("🗑️  [对话状态] 删除状态: sessionId={}, 轮次={}", 
                    sessionId, removed.getRoundCount());
        }
    }
    
    /**
     * 检查是否存在对话状态
     * 
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean hasState(String sessionId) {
        return sessionId != null && activeStates.containsKey(sessionId);
    }
    
    /**
     * 获取活跃状态数量
     * 
     * @return 数量
     */
    public int getActiveStateCount() {
        return activeStates.size();
    }
    
    /**
     * 清理过期的状态（定时任务，每小时执行一次）
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void cleanupExpiredStates() {
        List<String> expiredSessionIds = new ArrayList<>();
        
        for (Map.Entry<String, DialogueState> entry : activeStates.entrySet()) {
            DialogueState state = entry.getValue();
            if (state.isExpired(DEFAULT_TIMEOUT_MS)) {
                expiredSessionIds.add(entry.getKey());
            }
        }
        
        if (!expiredSessionIds.isEmpty()) {
            log.info("🧹 [对话状态] 清理过期状态: 数量={}", expiredSessionIds.size());
            expiredSessionIds.forEach(activeStates::remove);
        }
    }
    
    /**
     * 清理指定状态的状态（例如已完成、已失败）
     */
    public void cleanupByStatus(DialogueState.StateStatus status) {
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, DialogueState> entry : activeStates.entrySet()) {
            if (entry.getValue().getStatus() == status) {
                toRemove.add(entry.getKey());
            }
        }
        
        if (!toRemove.isEmpty()) {
            log.info("🧹 [对话状态] 清理指定状态: status={}, 数量={}", status, toRemove.size());
            toRemove.forEach(activeStates::remove);
        }
    }
    
    /**
     * 清空所有状态（慎用）
     */
    public void clearAll() {
        int count = activeStates.size();
        activeStates.clear();
        log.warn("🗑️  [对话状态] 清空所有状态: 数量={}", count);
    }
    
    /**
     * 获取统计信息
     */
    public StateStatistics getStatistics() {
        StateStatistics stats = new StateStatistics();
        
        for (DialogueState state : activeStates.values()) {
            stats.total++;
            
            switch (state.getStatus()) {
                case PARAMETER_COLLECTING:
                    stats.collecting++;
                    break;
                case COMPLETED:
                    stats.completed++;
                    break;
                case FAILED:
                    stats.failed++;
                    break;
                case CANCELLED:
                    stats.cancelled++;
                    break;
            }
            
            // 统计轮次
            if (state.getRoundCount() > stats.maxRounds) {
                stats.maxRounds = state.getRoundCount();
            }
            stats.totalRounds += state.getRoundCount();
        }
        
        if (stats.total > 0) {
            stats.avgRounds = (double) stats.totalRounds / stats.total;
        }
        
        return stats;
    }
    
    /**
     * 应用关闭时清理资源
     */
    @PreDestroy
    public void destroy() {
        log.info("🔚 [对话状态] 应用关闭，清理所有状态: 数量={}", activeStates.size());
        activeStates.clear();
    }
    
    /**
     * 状态统计信息
     */
    public static class StateStatistics {
        public int total = 0;
        public int collecting = 0;
        public int completed = 0;
        public int failed = 0;
        public int cancelled = 0;
        public int maxRounds = 0;
        public int totalRounds = 0;
        public double avgRounds = 0.0;
        
        @Override
        public String toString() {
            return String.format(
                "总数=%d, 收集中=%d, 已完成=%d, 已失败=%d, 已取消=%d, 平均轮次=%.1f",
                total, collecting, completed, failed, cancelled, avgRounds
            );
        }
    }
}

