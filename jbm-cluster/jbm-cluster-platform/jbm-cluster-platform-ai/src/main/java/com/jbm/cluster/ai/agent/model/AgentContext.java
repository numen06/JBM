package com.jbm.cluster.ai.agent.model;

import com.jbm.cluster.ai.agent.dialogue.DialogueState;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent 上下文
 * 
 * 存储请求处理过程中的所有上下文信息
 * 
 * @author wesley
 */
@Data
public class AgentContext {
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 用户问题
     */
    private String userQuery;
    
    /**
     * 识别出的意图
     */
    private Intent intent;
    
    /**
     * 选中的 API
     */
    private ApiDefinition selectedApi;
    
    /**
     * 绑定参数后的 URL
     */
    private String boundUrl;
    
    /**
     * 请求方法
     */
    private String requestMethod;
    
    /**
     * 请求体（用于 POST/PUT）
     */
    private String requestBody;
    
    /**
     * API 响应
     */
    private String apiResponse;
    
    /**
     * API 响应状态码
     */
    private int responseStatusCode;
    
    /**
     * 是否成功
     */
    private boolean success = false;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 处理阶段
     */
    private ProcessStage currentStage;
    
    /**
     * 额外的上下文数据
     */
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 对话状态（用于参数收集）
     */
    private DialogueState dialogueState;
    
    /**
     * 是否需要参数收集
     */
    private boolean needsParameterCollection = false;
    
    /**
     * 已收集的参数
     */
    private Map<String, Object> collectedParameters = new HashMap<>();
    
    /**
     * 构造函数
     */
    public AgentContext(String sessionId, String userQuery) {
        this.sessionId = sessionId;
        this.userQuery = userQuery;
        this.startTime = LocalDateTime.now();
        this.currentStage = ProcessStage.INITIALIZED;
    }
    
    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * 设置当前阶段
     */
    public void setStage(ProcessStage stage) {
        this.currentStage = stage;
    }
    
    /**
     * 标记完成
     */
    public void markCompleted(boolean success) {
        this.success = success;
        this.endTime = LocalDateTime.now();
        this.currentStage = success ? ProcessStage.COMPLETED : ProcessStage.FAILED;
    }
    
    /**
     * 获取处理耗时（毫秒）
     */
    public long getDurationMs() {
        if (startTime == null) {
            return 0;
        }
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMillis();
    }
    
    /**
     * 处理阶段枚举
     */
    public enum ProcessStage {
        INITIALIZED("初始化"),
        NLU("自然语言理解"),
        ROUTING("意图路由"),
        API_SELECTION("API选择"),
        PARAMETER_COLLECTION("参数收集"),
        PARAMETER_BINDING("参数绑定"),
        API_CALLING("API调用"),
        RESPONSE_FORMATTING("响应格式化"),
        COMPLETED("完成"),
        FAILED("失败");
        
        private final String description;
        
        ProcessStage(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}

