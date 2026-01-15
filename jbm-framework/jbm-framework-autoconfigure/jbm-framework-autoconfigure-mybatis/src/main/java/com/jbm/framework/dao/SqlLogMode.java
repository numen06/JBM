package com.jbm.framework.dao;

/**
 * SQL 日志记录模式枚举
 * 
 * @author wesley
 */
public enum SqlLogMode {
    /**
     * 白名单模式：只记录白名单中匹配的 SQL
     * 需要配置 whitelist
     */
    WHITELIST,
    
    /**
     * 普通模式：记录所有 SQL（除了被过滤的）
     * 不需要配置 whitelist，会记录所有 SQL
     */
    NORMAL
}
