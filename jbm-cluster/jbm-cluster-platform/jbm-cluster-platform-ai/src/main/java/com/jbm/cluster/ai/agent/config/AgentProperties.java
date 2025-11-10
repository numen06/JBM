package com.jbm.cluster.ai.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 配置属性
 * 
 * @author wesley
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {
    
    /**
     * NLU 配置
     */
    private NluConfig nlu = new NluConfig();
    
    /**
     * API Selection 配置
     */
    private SelectionConfig selection = new SelectionConfig();
    
    /**
     * Execution 配置
     */
    private ExecutionConfig execution = new ExecutionConfig();
    
    /**
     * 对话配置
     */
    private DialogueConfig dialogue = new DialogueConfig();
    
    /**
     * NLU 配置
     */
    @Data
    public static class NluConfig {
        /**
         * 使用的模型
         */
        private String model = "qwen-max";
        
        /**
         * 温度参数
         */
        private Double temperature = 0.1;
        
        /**
         * 最大 tokens
         */
        private Integer maxTokens = 500;
        
        /**
         * API 概述中最多显示的服务数量
         */
        private Integer maxServices = 10;
        
        /**
         * 每个服务最多显示的 API 数量
         */
        private Integer maxApisPerService = 5;
    }
    
    /**
     * API Selection 配置
     */
    @Data
    public static class SelectionConfig {
        /**
         * 匹配分数阈值
         */
        private Double matchThreshold = 0.3;
        
        /**
         * 缓存有效期（秒）
         */
        private Integer cacheDuration = 60;
    }
    
    /**
     * Execution 配置
     */
    @Data
    public static class ExecutionConfig {
        /**
         * 超时时间（秒）
         */
        private Integer timeout = 30;
        
        /**
         * 重试次数
         */
        private Integer retries = 0;
    }
    
    /**
     * 对话配置
     */
    @Data
    public static class DialogueConfig {
        /**
         * 是否启用对话模式
         */
        private Boolean enabled = true;
        
        /**
         * 最大对话轮次
         */
        private Integer maxRounds = 5;
        
        /**
         * 会话超时时间（小时）
         */
        private Integer sessionTimeoutHours = 24;
        
        /**
         * 是否自动推断参数
         */
        private Boolean autoInfer = true;
        
        /**
         * 参数提取的最低置信度阈值
         */
        private Double extractionConfidenceThreshold = 0.5;
    }
}

