package com.jbm.cluster.ai.model;

import lombok.Data;

/**
 * 聊天响应
 * @author wesley
 */
@Data
public class ChatResponse {
    /**
     * AI 回复内容
     */
    private String message;
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 是否调用了函数
     */
    private boolean functionCalled;
    
    /**
     * 调用的函数名
     */
    private String functionName;
    
    /**
     * 函数调用结果
     */
    private String functionResult;
    
    /**
     * 错误信息
     */
    private String error;
}

