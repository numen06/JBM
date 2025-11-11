package com.jbm.cluster.api.model.log;

/**
 * 业务日志事件类型枚举
 * 
 * @author wesley
 */
public enum BusinessLogEventType {
    
    /**
     * 创建日志
     */
    CREATE,
    
    /**
     * 追加日志内容
     */
    APPEND,
    
    /**
     * 删除日志（标记删除）
     */
    DELETE,
    
    /**
     * 更新过期时间
     */
    UPDATE_EXPIRE,
    
    /**
     * 生成临时访问URL
     */
    GENERATE_URL,
    
    /**
     * 查询日志
     */
    QUERY
}

