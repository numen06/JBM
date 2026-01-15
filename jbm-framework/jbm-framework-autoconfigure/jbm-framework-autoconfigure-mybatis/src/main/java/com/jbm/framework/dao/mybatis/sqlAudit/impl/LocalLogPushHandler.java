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
            
            // 统一使用格式化字符串，一行打印
            String customFormat = sqlLogProperties.getCustomFormat();
            if (StrUtil.isBlank(customFormat)) {
                // 如果没有配置，使用默认格式
                customFormat = "%(currentTime) | DS: %(dataSource) | took %(executionTime)ms | %(sql)";
            }
            
            String formattedLog = SqlLogFormatter.format(customFormat, executionInfo);
            if (StrUtil.isNotBlank(formattedLog)) {
                log.info(formattedLog);
            }
        } catch (Exception e) {
            log.error("本地日志推送失败", e);
        }
    }
    
    @Override
    public boolean isEnabled() {
        return true; // 本地打印默认始终启用
    }
}
