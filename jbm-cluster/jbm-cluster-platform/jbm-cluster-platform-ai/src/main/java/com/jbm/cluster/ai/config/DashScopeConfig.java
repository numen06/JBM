package com.jbm.cluster.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DashScope 配置
 * @author wesley
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dashscope")
public class DashScopeConfig {
    
    /**
     * API Key
     */
    private String apiKey = "sk-default";
    
    /**
     * 模型名称
     */
    private String model = "qwen-plus";
    
    /**
     * 温度参数 (0-1)
     */
    private Double temperature = 0.7;
    
    /**
     * 最大输出 tokens
     */
    private Integer maxTokens = 2000;
    
    /**
     * 是否启用搜索
     */
    private Boolean enableSearch = false;
    
    /**
     * 是否启用 Agent 模式
     * true: 使用 4 个元函数（推荐，Token 消耗少）
     * false: 注册所有 API 为函数（Token 消耗大）
     */
    private Boolean agentMode = true;
}

