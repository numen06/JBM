package com.jbm.framework.dao.mybatis.sqlAudit.impl;

import com.jbm.framework.dao.mybatis.sqlAudit.SqlAuditPushHandler;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlAuditPushType;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlExecutionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP 推送处理器
 * 
 * @author wesley
 */
public class HttpPushHandler implements SqlAuditPushHandler {
    private static final Logger log = LoggerFactory.getLogger(HttpPushHandler.class);
    
    private final boolean enabled;
    private final String url;
    
    public HttpPushHandler(boolean enabled, String url) {
        this.enabled = enabled;
        this.url = url;
    }
    
    @Override
    public SqlAuditPushType getPushType() {
        return SqlAuditPushType.HTTP;
    }
    
    @Override
    public void push(SqlExecutionInfo executionInfo) {
        if (!isEnabled() || executionInfo == null) {
            return;
        }
        
        try {
            // TODO: 实现 HTTP 推送逻辑
            // 例如：
            // RestTemplate restTemplate = new RestTemplate();
            // restTemplate.postForObject(url, convertToJson(executionInfo), String.class);
            // 或使用异步方式
            
            String slowQueryInfo = executionInfo.getSlowQuery() != null && executionInfo.getSlowQuery() 
                    ? String.format(" [慢查询: %sms > %sms]", 
                            executionInfo.getExecutionTime(), executionInfo.getSlowQueryThreshold())
                    : "";
            log.debug("HTTP 推送 SQL 审计信息: application={}, instanceId={}, mapperId={}, executionTime={}ms{}, url={}", 
                    executionInfo.getApplicationName(), executionInfo.getInstanceId(), 
                    executionInfo.getMapperId(), executionInfo.getExecutionTime(), slowQueryInfo, url);
        } catch (Exception e) {
            log.error("HTTP 推送 SQL 审计信息失败", e);
        }
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
