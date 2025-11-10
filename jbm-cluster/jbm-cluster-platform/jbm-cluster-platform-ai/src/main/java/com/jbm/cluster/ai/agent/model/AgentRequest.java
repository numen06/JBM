package com.jbm.cluster.ai.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 请求
 * 
 * 包装 Agent 服务的请求参数
 * 
 * @author wesley
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {
    
    /**
     * 用户消息/问题
     */
    private String message;
    
    /**
     * 会话 ID（可选）
     */
    private String sessionId;
    
    /**
     * 是否启用 Agent 功能
     */
    @Builder.Default
    private boolean enableAgent = true;
    
    /**
     * 是否返回详细信息（包含中间步骤）
     */
    @Builder.Default
    private boolean verbose = false;
}

