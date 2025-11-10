package com.jbm.cluster.ai.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户意图
 * 
 * 表示从用户问题中识别出的意图和提取的参数
 * 
 * @author wesley
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Intent {
    
    /**
     * 意图名称
     * 例如：query_user_info, list_online_users, get_system_status
     */
    private String name;
    
    /**
     * 置信度（0-1）
     * 表示 NLU 模块对意图识别的信心程度
     */
    private double confidence;
    
    /**
     * 提取的参数
     * 例如：{"userId": "123", "timeRange": "300s"}
     */
    @Builder.Default
    private Map<String, Object> params = new HashMap<>();
    
    /**
     * 原始用户问题
     */
    private String rawQuery;
    
    /**
     * 意图类型
     * 用于快速分类：QUERY(查询), CREATE(创建), UPDATE(更新), DELETE(删除), OTHER(其他)
     */
    private IntentType type;
    
    /**
     * 添加参数
     */
    public void addParam(String key, Object value) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }
        this.params.put(key, value);
    }
    
    /**
     * 获取参数
     */
    public Object getParam(String key) {
        return params != null ? params.get(key) : null;
    }
    
    /**
     * 获取字符串参数
     */
    public String getStringParam(String key) {
        Object value = getParam(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 意图类型枚举
     */
    public enum IntentType {
        QUERY,   // 查询
        CREATE,  // 创建
        UPDATE,  // 更新
        DELETE,  // 删除
        OTHER    // 其他
    }
}

