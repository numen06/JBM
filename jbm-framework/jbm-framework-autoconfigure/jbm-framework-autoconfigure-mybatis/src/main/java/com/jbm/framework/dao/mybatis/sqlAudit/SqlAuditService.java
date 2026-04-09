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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SQL 审计服务
 * 负责管理多种推送方式的处理器，并根据配置选择合适的推送方式
 * 
 * @author wesley
 */
public class SqlAuditService {
    private static final Logger log = LoggerFactory.getLogger(SqlAuditService.class);
    
    private final List<SqlAuditPushHandler> handlers = new ArrayList<>();
    private ExecutorService executorService;
    private final boolean enabled;
    private final SqlLogProperties.SqlAuditProperties auditProperties;
    
    public SqlAuditService(SqlLogProperties sqlLogProperties) {
        this.auditProperties = sqlLogProperties.getAudit();
        this.enabled = auditProperties != null && auditProperties.getEnabled() != null && auditProperties.getEnabled();
        
        if (!enabled) {
            return;
        }
        
        // 创建带名称的线程池，便于监控和排查
        // 核心线程数2，最大线程数5，空闲60秒回收，队列容量100，拒绝策略为丢弃（审计日志不应阻塞主流程）
        this.executorService = new ThreadPoolExecutor(
            2, 5, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new SqlAuditThreadFactory(),
            new ThreadPoolExecutor.DiscardPolicy()
        );
        
        // 初始化推送处理器
        initializeHandlers(sqlLogProperties, auditProperties);
    }
    
    /**
     * SQL 审计线程工厂，为线程设置有意义的名称便于排查
     */
    private static class SqlAuditThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "sql-audit-" + counter.incrementAndGet());
            thread.setDaemon(true); // 守护线程，不阻塞 JVM 关闭
            return thread;
        }
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
            try {
                // 等待已提交的任务在3秒内完成
                if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                    log.warn("SQL 审计线程池未在超时时间内关闭，执行强制关闭");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
