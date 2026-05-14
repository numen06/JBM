package com.jbm.framework.dao.mybatis.sqlAudit;

/**
 * SQL 审计推送方式枚举
 * 
 * @author wesley
 */
public enum SqlAuditPushType {
    /**
     * 本地打印（默认方式）
     */
    LOCAL_LOG,
    
    /**
     * 数据库存储
     */
    DATABASE,
    
    /**
     * 消息队列（如 Kafka、RabbitMQ）
     */
    MESSAGE_QUEUE,
    
    /**
     * HTTP 推送
     */
    HTTP,
    
    /**
     * 多种方式组合
     */
    MULTIPLE
}
