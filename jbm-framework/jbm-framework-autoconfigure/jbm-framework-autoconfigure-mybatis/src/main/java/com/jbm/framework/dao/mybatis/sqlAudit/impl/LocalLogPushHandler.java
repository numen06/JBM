package com.jbm.framework.dao.mybatis.sqlAudit.impl;

import com.jbm.framework.dao.SqlLogFormat;
import com.jbm.framework.dao.SqlLogProperties;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlAuditPushHandler;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlAuditPushType;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlExecutionInfo;
import com.jbm.framework.dao.mybatis.sqlInjector.ReadableSqlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 本地日志打印推送处理器（默认方式）
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
        
        SqlLogFormat format = sqlLogProperties.getFormat();
        if (format == null) {
            format = SqlLogFormat.MERGED;
        }
        
        try {
            if (format == SqlLogFormat.OFFICIAL) {
                // 官方格式：完整输出 Preparing、Parameters 和结果信息
                // 注意：官方格式在执行后统一输出所有信息
                String sql = executionInfo.getOriginalSql();
                log.info("==>  Preparing: {}", sql.endsWith(";") ? sql : sql + ";");
                
                if (executionInfo.getParametersFormatted() != null && !executionInfo.getParametersFormatted().isEmpty()) {
                    log.info("==> Parameters: {}", executionInfo.getParametersFormatted());
                }
                
                // 输出结果信息（如果是查询且配置了显示）
                if ("query".equals(executionInfo.getOperationType())) {
                    Boolean showColumns = sqlLogProperties.getShowColumns();
                    Boolean showRows = sqlLogProperties.getShowRows();
                    Boolean showTotal = sqlLogProperties.getShowTotal();
                    
                    if ((showColumns != null && showColumns) || 
                        (showRows != null && showRows) || 
                        (showTotal != null && showTotal)) {
                        
                        List<String> resultLines = ReadableSqlUtil.formatResultForOfficial(
                            executionInfo.getResult(), 
                            showColumns != null && showColumns,
                            showRows != null && showRows,
                            showTotal != null && showTotal
                        );
                        for (String line : resultLines) {
                            log.info(line);
                        }
                    }
                }
            } else if (format == SqlLogFormat.MERGED) {
                // 合并格式：输出包含执行时间和mapper信息的完整日志
                if (executionInfo.getReadableSql() != null && executionInfo.getExecutionTime() != null) {
                    String sql = executionInfo.getReadableSql().endsWith(";") ? executionInfo.getReadableSql() : executionInfo.getReadableSql() + ";";
                    log.info("[SQL Run Time : {} ms ],[SQL Mapper : {}] \n {}", 
                            executionInfo.getExecutionTime(), executionInfo.getMapperId(), sql);
                }
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
