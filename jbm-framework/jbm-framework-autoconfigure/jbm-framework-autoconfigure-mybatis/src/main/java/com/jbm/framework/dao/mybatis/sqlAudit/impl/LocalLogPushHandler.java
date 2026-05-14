package com.jbm.framework.dao.mybatis.sqlAudit.impl;

import cn.hutool.core.util.StrUtil;
import com.jbm.framework.dao.SqlLogProperties;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlAuditPushHandler;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlAuditPushType;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlExecutionInfo;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本地日志打印推送处理器（默认方式）
 * 统一使用一行打印格式，便于日志采集
 * 
 * @author wesley
 */
public class LocalLogPushHandler implements SqlAuditPushHandler {
    private static final Logger log = LoggerFactory.getLogger(LocalLogPushHandler.class);
    
    private final SqlLogProperties sqlLogProperties;
    
    public LocalLogPushHandler(SqlLogProperties sqlLogProperties) {
        this.sqlLogProperties = sqlLogProperties;
    }
    
    @Override
    public SqlAuditPushType getPushType() {
        return SqlAuditPushType.LOCAL_LOG;
    }
    
    @Override
    public void push(SqlExecutionInfo executionInfo) {
        if (executionInfo == null) {
            return;
        }
        
        try {
            // 检查是否为慢查询且需要打印慢查询日志
            SqlLogProperties.SlowQueryProperties slowQueryProps = sqlLogProperties.getSlowQuery();
            boolean isSlowQuery = executionInfo.getSlowQuery() != null && executionInfo.getSlowQuery();
            boolean shouldLogSlowQuery = slowQueryProps != null && 
                                       (slowQueryProps.getLogSlowQuery() == null || slowQueryProps.getLogSlowQuery());
            
            // 如果是慢查询，输出慢查询警告（单独一行）
            if (isSlowQuery && shouldLogSlowQuery) {
                log.warn("⚠️ [慢查询警告] SQL执行时间: {} ms，超过阈值: {} ms", 
                        executionInfo.getExecutionTime(), executionInfo.getSlowQueryThreshold());
            }
            
            // 如果是失败的 SQL 且需要简化输出，则简化 SQL 内容
            SqlExecutionInfo logInfo = executionInfo;
            if (executionInfo.getSuccess() != null && !executionInfo.getSuccess()) {
                Boolean simplifyFailedSqlLog = sqlLogProperties.getSimplifyFailedSqlLog();
                if (simplifyFailedSqlLog != null && simplifyFailedSqlLog) {
                    // 创建简化版本的信息对象
                    logInfo = simplifyFailedSqlInfo(executionInfo);
                }
            }
            
            // 统一使用格式化字符串，一行打印
            String customFormat = sqlLogProperties.getCustomFormat();
            if (StrUtil.isBlank(customFormat)) {
                // 如果没有配置，使用默认格式（包含执行结果）
                customFormat = "%(currentTime) | DS: %(dataSource) | [%(result)] | took %(executionTime)ms | %(sql)%(errorMessage)";
            }
            
            String formattedLog = SqlLogFormatter.format(customFormat, logInfo);
            if (StrUtil.isNotBlank(formattedLog)) {
                // 失败的 SQL 使用 warn 级别，成功的使用 info 级别
                if (executionInfo.getSuccess() != null && !executionInfo.getSuccess()) {
                    log.warn(formattedLog);
                } else {
                    log.info(formattedLog);
                }
            }
        } catch (Exception e) {
            log.error("本地日志推送失败", e);
        }
    }
    
    /**
     * 简化失败 SQL 的信息，避免输出过长
     */
    private SqlExecutionInfo simplifyFailedSqlInfo(SqlExecutionInfo original) {
        SqlExecutionInfo simplified = new SqlExecutionInfo();
        // 复制基本信息
        simplified.setMapperId(original.getMapperId());
        simplified.setOperationType(original.getOperationType());
        simplified.setExecutionTime(original.getExecutionTime());
        simplified.setSuccess(original.getSuccess());
        simplified.setErrorMessage(original.getErrorMessage());
        simplified.setStartTime(original.getStartTime());
        simplified.setEndTime(original.getEndTime());
        simplified.setApplicationName(original.getApplicationName());
        simplified.setInstanceId(original.getInstanceId());
        simplified.setHostname(original.getHostname());
        simplified.setIp(original.getIp());
        simplified.setPort(original.getPort());
        simplified.setSlowQuery(original.getSlowQuery());
        simplified.setSlowQueryThreshold(original.getSlowQueryThreshold());
        
        // 简化 SQL：只保留前 100 个字符
        String sql = original.getReadableSql();
        if (StrUtil.isBlank(sql)) {
            sql = original.getOriginalSql();
        }
        if (StrUtil.isNotBlank(sql)) {
            sql = sql.trim();
            // 移除换行符和多余空格
            sql = sql.replaceAll("\\s+", " ");
            // 如果超过 100 个字符，截断
            if (sql.length() > 100) {
                sql = sql.substring(0, 100) + "...";
            }
            simplified.setReadableSql(sql);
            simplified.setOriginalSql(sql);
        }
        
        return simplified;
    }
    
    @Override
    public boolean isEnabled() {
        return true; // 本地打印默认始终启用
    }
}
