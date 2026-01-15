package com.jbm.framework.dao.mybatis.sqlAudit;

/**
 * SQL 审计推送处理器接口
 * 实现此接口以支持不同的推送方式
 * 
 * @author wesley
 */
public interface SqlAuditPushHandler {
    
    /**
     * 获取推送方式类型
     * 
     * @return 推送方式类型
     */
    SqlAuditPushType getPushType();
    
    /**
     * 推送 SQL 执行信息
     * 
     * @param executionInfo SQL 执行信息
     */
    void push(SqlExecutionInfo executionInfo);
    
    /**
     * 是否启用此推送方式
     * 
     * @return true 启用，false 禁用
     */
    boolean isEnabled();
}
