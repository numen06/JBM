package com.jbm.cluster.ai.agent.routing;

import com.jbm.cluster.ai.agent.model.Intent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;

/**
 * 意图路由器
 * 
 * 根据意图选择合适的处理器
 * 使用责任链模式
 * 
 * @author wesley
 */
@Slf4j
@Component
public class IntentRouter {
    
    @Autowired
    private List<IntentHandler> handlers;
    
    /**
     * 初始化：按优先级排序处理器
     */
    @PostConstruct
    public void init() {
        if (handlers != null && !handlers.isEmpty()) {
            handlers.sort(Comparator.comparingInt(IntentHandler::getPriority).reversed());
            log.info("🎯 [Intent Router] 初始化完成，共 {} 个处理器", handlers.size());
            handlers.forEach(handler -> {
                log.info("   - {}: 优先级 {}", 
                        handler.getClass().getSimpleName(), 
                        handler.getPriority());
            });
        } else {
            log.warn("⚠️  [Intent Router] 未找到任何意图处理器");
        }
    }
    
    /**
     * 路由意图到合适的处理器
     * 
     * @param intent 用户意图
     * @return 能够处理该意图的处理器
     */
    public IntentHandler route(Intent intent) {
        if (intent == null) {
            log.warn("⚠️  [Intent Router] 意图为空");
            return null;
        }
        
        log.info("🎯 [Intent Router] 路由意图: {}", intent.getName());
        
        if (handlers == null || handlers.isEmpty()) {
            log.error("❌ [Intent Router] 没有可用的处理器");
            return null;
        }
        
        // 按优先级依次尝试处理器
        for (IntentHandler handler : handlers) {
            if (handler.canHandle(intent)) {
                log.info("✅ [Intent Router] 选择处理器: {}", 
                        handler.getClass().getSimpleName());
                return handler;
            }
        }
        
        log.warn("⚠️  [Intent Router] 未找到能处理该意图的处理器");
        return null;
    }
    
    /**
     * 获取所有处理器
     */
    public List<IntentHandler> getAllHandlers() {
        return handlers;
    }
}

