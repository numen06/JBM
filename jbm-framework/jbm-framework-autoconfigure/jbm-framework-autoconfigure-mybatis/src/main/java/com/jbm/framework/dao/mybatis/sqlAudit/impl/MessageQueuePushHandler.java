package com.jbm.framework.dao.mybatis.sqlAudit.impl;

import com.jbm.framework.dao.mybatis.sqlAudit.SqlAuditPushHandler;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlAuditPushType;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlExecutionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 消息队列推送处理器（如 Kafka、RabbitMQ）
 * 
 * @author wesley
 */
public class MessageQueuePushHandler implements SqlAuditPushHandler {
    private static final Logger log = LoggerFactory.getLogger(MessageQueuePushHandler.class);
    
    private final boolean enabled;
    private final String topic;
    private final String exchange;
    
    public MessageQueuePushHandler(boolean enabled, String topic, String exchange) {
        this.enabled = enabled;
        this.topic = topic;
        this.exchange = exchange;
    }
    
    @Override
    public SqlAuditPushType getPushType() {
        return SqlAuditPushType.MESSAGE_QUEUE;
    }
    
    @Override
    public void push(SqlExecutionInfo executionInfo) {
        if (!isEnabled() || executionInfo == null) {
            return;
        }
        
        try {
            // TODO: 实现消息队列推送逻辑
            // 例如：
            // kafkaTemplate.send(topic, convertToJson(executionInfo));
            // 或
            // rabbitTemplate.convertAndSend(exchange, routingKey, executionInfo);
            
            String slowQueryInfo = executionInfo.getSlowQuery() != null && executionInfo.getSlowQuery() 
                    ? String.format(" [慢查询: %sms > %sms]", 
                            executionInfo.getExecutionTime(), executionInfo.getSlowQueryThreshold())
                    : "";
            log.debug("消息队列推送 SQL 审计信息: application={}, instanceId={}, mapperId={}, executionTime={}ms{}, topic={}", 
                    executionInfo.getApplicationName(), executionInfo.getInstanceId(), 
                    executionInfo.getMapperId(), executionInfo.getExecutionTime(), slowQueryInfo, topic);
        } catch (Exception e) {
            log.error("消息队列推送 SQL 审计信息失败", e);
        }
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
