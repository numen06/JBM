package com.jbm.framework.dao;

/**
 * SQL日志格式类型枚举
 * 
 * @author wesley
 */
public enum SqlLogFormat {
    /**
     * 合并格式：SQL和参数合并输出，直接显示替换参数后的SQL
     * 格式：==>  Preparing: SELECT ... WHERE id = 123;
     */
    MERGED,
    
    /**
     * 官方格式：分别显示Preparing和Parameters
     * 格式：
     * ==>  Preparing: SELECT ... WHERE id = ?;
     * ==> Parameters: 123(String)
     */
    OFFICIAL
}

