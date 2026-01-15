package com.jbm.framework.dao.mybatis.sqlAudit.impl;

import com.jbm.framework.dao.mybatis.sqlAudit.SqlAuditPushHandler;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlAuditPushType;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlExecutionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据库存储推送处理器
 * 
 * @author wesley
 */
public class DatabasePushHandler implements SqlAuditPushHandler {
    private static final Logger log = LoggerFactory.getLogger(DatabasePushHandler.class);
    
    private final boolean enabled;
    
    public DatabasePushHandler(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public SqlAuditPushType getPushType() {
        return SqlAuditPushType.DATABASE;
    }
    
    @Override
    public void push(SqlExecutionInfo executionInfo) {
        if (!isEnabled() || executionInfo == null) {
            return;
        }
        
        try {
            // TODO: 实现数据库存储逻辑
            // 例如：使用 JPA 或 MyBatis 将 executionInfo 保存到数据库
            // sqlAuditRepository.save(convertToEntity(executionInfo));
            
            String slowQueryInfo = executionInfo.getSlowQuery() != null && executionInfo.getSlowQuery() 
                    ? String.format(" [慢查询: %sms > %sms]", 
                            executionInfo.getExecutionTime(), executionInfo.getSlowQueryThreshold())
                    : "";
            log.debug("数据库推送 SQL 审计信息: application={}, instanceId={}, mapperId={}, executionTime={}ms{}", 
                    executionInfo.getApplicationName(), executionInfo.getInstanceId(), 
                    executionInfo.getMapperId(), executionInfo.getExecutionTime(), slowQueryInfo);
        } catch (Exception e) {
            log.error("数据库推送 SQL 审计信息失败", e);
        }
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
