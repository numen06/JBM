package com.jbm.cluster.ai.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 响应
 * 
 * Agent 服务的响应结果
 * 
 * @author wesley
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * AI 回复内容
     */
    private String message;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 处理耗时（毫秒）
     */
    private long durationMs;
    
    /**
     * 识别出的意图（可选，verbose 模式）
     */
    private Intent intent;
    
    /**
     * 调用的 API（可选，verbose 模式）
     */
    private String apiCalled;
    
    /**
     * 构造成功响应
     */
    public static AgentResponse success(String sessionId, String message) {
        return AgentResponse.builder()
                .sessionId(sessionId)
                .message(message)
                .success(true)
                .build();
    }
    
    /**
     * 构造失败响应
     */
    public static AgentResponse error(String sessionId, String error) {
        return AgentResponse.builder()
                .sessionId(sessionId)
                .error(error)
                .success(false)
                .build();
    }
}

