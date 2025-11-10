package com.jbm.cluster.ai.agent.dialogue;

import com.jbm.cluster.ai.agent.model.ApiDefinition;
import com.jbm.cluster.ai.agent.model.Intent;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 对话状态
 * 
 * 存储多轮对话过程中的状态信息，用于参数收集
 * 
 * @author wesley
 */
@Data
public class DialogueState {
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 用户原始问题
     */
    private String originalQuery;
    
    /**
     * 识别的意图
     */
    private Intent intent;
    
    /**
     * 选中的 API
     */
    private ApiDefinition selectedApi;
    
    /**
     * 已收集的参数
     */
    private Map<String, Object> collectedParameters = new HashMap<>();
    
    /**
     * 缺失的必填参数列表
     */
    private List<String> missingRequiredParameters = new ArrayList<>();
    
    /**
     * 当前正在询问的参数
     */
    private String currentAskingParameter;
    
    /**
     * 对话轮次（从1开始）
     */
    private int roundCount = 0;
    
    /**
     * 对话历史
     */
    private List<DialogueRound> dialogueHistory = new ArrayList<>();
    
    /**
     * 状态
     */
    private StateStatus status = StateStatus.PARAMETER_COLLECTING;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 构造函数
     */
    public DialogueState(String sessionId, String originalQuery, Intent intent, ApiDefinition selectedApi) {
        this.sessionId = sessionId;
        this.originalQuery = originalQuery;
        this.intent = intent;
        this.selectedApi = selectedApi;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // 从意图中提取已有参数
        if (intent.getParams() != null) {
            this.collectedParameters.putAll(intent.getParams());
        }
    }
    
    /**
     * 添加收集的参数
     */
    public void addCollectedParameter(String name, Object value) {
        this.collectedParameters.put(name, value);
        this.missingRequiredParameters.remove(name);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 批量添加收集的参数
     */
    public void addCollectedParameters(Map<String, Object> params) {
        if (params != null && !params.isEmpty()) {
            this.collectedParameters.putAll(params);
            params.keySet().forEach(this.missingRequiredParameters::remove);
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 设置缺失的参数列表
     */
    public void setMissingRequiredParameters(List<String> missing) {
        this.missingRequiredParameters = new ArrayList<>(missing);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 记录一轮对话
     */
    public void recordRound(String question, String userAnswer) {
        this.roundCount++;
        DialogueRound round = new DialogueRound(
                this.roundCount,
                this.currentAskingParameter,
                question,
                userAnswer,
                LocalDateTime.now()
        );
        this.dialogueHistory.add(round);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 检查是否还在等待参数
     */
    public boolean isWaitingForParameter() {
        return status == StateStatus.PARAMETER_COLLECTING && 
               !missingRequiredParameters.isEmpty();
    }
    
    /**
     * 检查参数是否已齐全
     */
    public boolean isParametersComplete() {
        return missingRequiredParameters.isEmpty();
    }
    
    /**
     * 标记为完成
     */
    public void markComplete() {
        this.status = StateStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为失败
     */
    public void markFailed(String reason) {
        this.status = StateStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为取消
     */
    public void markCancelled() {
        this.status = StateStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 检查是否过期（超过指定时间未更新）
     */
    public boolean isExpired(long timeoutMillis) {
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(updatedAt, now).toMillis() > timeoutMillis;
    }
    
    /**
     * 获取下一个需要询问的参数
     */
    public String getNextParameterToAsk() {
        if (missingRequiredParameters.isEmpty()) {
            return null;
        }
        return missingRequiredParameters.get(0);
    }
    
    /**
     * 对话轮次记录
     */
    @Data
    public static class DialogueRound {
        /**
         * 轮次编号
         */
        private int roundNumber;
        
        /**
         * 询问的参数名
         */
        private String parameterName;
        
        /**
         * AI 的提问
         */
        private String question;
        
        /**
         * 用户的回答
         */
        private String userAnswer;
        
        /**
         * 时间
         */
        private LocalDateTime timestamp;
        
        public DialogueRound(int roundNumber, String parameterName, String question, 
                            String userAnswer, LocalDateTime timestamp) {
            this.roundNumber = roundNumber;
            this.parameterName = parameterName;
            this.question = question;
            this.userAnswer = userAnswer;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 状态枚举
     */
    public enum StateStatus {
        /**
         * 正在收集参数
         */
        PARAMETER_COLLECTING,
        
        /**
         * 已完成（参数齐全，可以执行）
         */
        COMPLETED,
        
        /**
         * 已失败（超过最大轮次或其他错误）
         */
        FAILED,
        
        /**
         * 已取消（用户主动取消）
         */
        CANCELLED
    }
}

