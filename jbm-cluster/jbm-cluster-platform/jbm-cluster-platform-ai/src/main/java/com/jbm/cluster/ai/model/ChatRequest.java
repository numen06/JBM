package com.jbm.cluster.ai.model;

import lombok.Data;

/**
 * 聊天请求
 * @author wesley
 */
@Data
public class ChatRequest {
    /**
     * 用户消息
     */
    private String message;
    
    /**
     * 会话 ID（可选，用于保持对话上下文）
     */
    private String sessionId;
    
    /**
     * 是否启用 Function Calling
     */
    private boolean enableFunctions = true;
}

