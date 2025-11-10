package com.jbm.cluster.ai.agent.routing;

import com.jbm.cluster.ai.agent.model.AgentContext;
import com.jbm.cluster.ai.agent.model.Intent;

/**
 * 意图处理器接口
 * 
 * 负责处理特定类型的意图
 * 
 * @author wesley
 */
public interface IntentHandler {
    
    /**
     * 判断是否能够处理该意图
     * 
     * @param intent 用户意图
     * @return true 如果可以处理
     */
    boolean canHandle(Intent intent);
    
    /**
     * 处理意图
     * 
     * @param context Agent 上下文
     * @return 更新后的上下文
     */
    AgentContext handle(AgentContext context);
    
    /**
     * 获取处理器优先级
     * 数值越大优先级越高
     * 
     * @return 优先级
     */
    default int getPriority() {
        return 0;
    }
}

