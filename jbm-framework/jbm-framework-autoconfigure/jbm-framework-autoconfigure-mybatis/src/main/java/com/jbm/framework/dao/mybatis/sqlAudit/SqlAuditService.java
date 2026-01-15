package com.jbm.framework.dao.mybatis.sqlAudit;

import com.jbm.framework.dao.SqlLogProperties;
import com.jbm.framework.dao.mybatis.sqlAudit.impl.DatabasePushHandler;
import com.jbm.framework.dao.mybatis.sqlAudit.impl.HttpPushHandler;
import com.jbm.framework.dao.mybatis.sqlAudit.impl.LocalLogPushHandler;
import com.jbm.framework.dao.mybatis.sqlAudit.impl.MessageQueuePushHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SQL 审计服务
 * 负责管理多种推送方式的处理器，并根据配置选择合适的推送方式
 * 
 * @author wesley
 */
public class SqlAuditService {
    private static final Logger log = LoggerFactory.getLogger(SqlAuditService.class);
    
    private final List<SqlAuditPushHandler> handlers = new ArrayList<>();
    private final ExecutorService executorService;
    private final boolean enabled;
    private final SqlLogProperties.SqlAuditProperties auditProperties;
    
    public SqlAuditService(SqlLogProperties sqlLogProperties) {
        this.auditProperties = sqlLogProperties.getAudit();
        this.enabled = auditProperties != null && auditProperties.getEnabled() != null && auditProperties.getEnabled();
        
        if (!enabled) {
            this.executorService = null;
            return;
        }
        
        // 创建线程池用于异步推送
        this.executorService = Executors.newFixedThreadPool(5);
        
        // 初始化推送处理器
        initializeHandlers(sqlLogProperties, auditProperties);
    }
    
    /**
     * 初始化推送处理器
     */
    private void initializeHandlers(SqlLogProperties sqlLogProperties, SqlLogProperties.SqlAuditProperties auditProperties) {
        // 本地打印处理器（默认启用）
        if (auditProperties.getEnableLocalLog() == null || auditProperties.getEnableLocalLog()) {
            handlers.add(new LocalLogPushHandler(sqlLogProperties));
        }
        
        // 数据库推送处理器
        if (auditProperties.getDatabase() != null && 
            auditProperties.getDatabase().getEnabled() != null && 
            auditProperties.getDatabase().getEnabled()) {
            handlers.add(new DatabasePushHandler(true));
        }
        
        // 消息队列推送处理器
        if (auditProperties.getMessageQueue() != null && 
            auditProperties.getMessageQueue().getEnabled() != null && 
            auditProperties.getMessageQueue().getEnabled()) {
            SqlLogProperties.SqlAuditProperties.MessageQueuePushProperties mqProps = auditProperties.getMessageQueue();
            handlers.add(new MessageQueuePushHandler(true, mqProps.getTopic(), mqProps.getExchange()));
        }
        
        // HTTP 推送处理器
        if (auditProperties.getHttp() != null && 
            auditProperties.getHttp().getEnabled() != null && 
            auditProperties.getHttp().getEnabled()) {
            SqlLogProperties.SqlAuditProperties.HttpPushProperties httpProps = auditProperties.getHttp();
            handlers.add(new HttpPushHandler(true, httpProps.getUrl()));
        }
    }
    
    /**
     * 推送 SQL 执行信息
     * 
     * @param executionInfo SQL 执行信息
     */
    public void push(SqlExecutionInfo executionInfo) {
        if (!enabled || executionInfo == null || handlers.isEmpty()) {
            return;
        }
        
        // 根据配置决定同步还是异步推送
        boolean async = auditProperties.getHttp() != null && 
                       auditProperties.getHttp().getAsync() != null && 
                       auditProperties.getHttp().getAsync();
        
        if (async) {
            // 异步推送
            CompletableFuture.runAsync(() -> {
                doPush(executionInfo);
            }, executorService).exceptionally(e -> {
                log.error("异步推送 SQL 审计信息失败", e);
                return null;
            });
        } else {
            // 同步推送
            doPush(executionInfo);
        }
    }
    
    /**
     * 执行推送
     */
    private void doPush(SqlExecutionInfo executionInfo) {
        for (SqlAuditPushHandler handler : handlers) {
            if (handler.isEnabled()) {
                try {
                    handler.push(executionInfo);
                } catch (Exception e) {
                    log.error("推送 SQL 审计信息失败，application={}, instanceId={}, 推送方式: {}", 
                            executionInfo.getApplicationName(), executionInfo.getInstanceId(), 
                            handler.getPushType(), e);
                }
            }
        }
    }
    
    /**
     * 关闭服务，释放资源
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
