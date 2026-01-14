package com.jbm.framework.dao.mybatis.sqlAudit;

import cn.hutool.core.date.StopWatch;
import com.jbm.framework.dao.SqlLogFormat;
import com.jbm.framework.dao.SqlLogProperties;
import com.jbm.framework.dao.mybatis.sqlInjector.ReadableSqlUtil;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SQL 拦截处理器
 * 负责收集 SQL 执行信息、格式化日志输出，并为后续日志审计和平台推送提供接口
 * 
 * @author wesley
 */
public class SqlInterceptorHandler {
    private static final Logger log = LoggerFactory.getLogger(SqlInterceptorHandler.class);
    
    private final SqlLogProperties sqlLogProperties;
    private final AntPathMatcher pathMatcher;
    private final SqlAuditService sqlAuditService;
    
    public SqlInterceptorHandler(SqlLogProperties sqlLogProperties) {
        this.sqlLogProperties = sqlLogProperties;
        this.pathMatcher = new AntPathMatcher();
        // 初始化审计服务
        this.sqlAuditService = new SqlAuditService(sqlLogProperties);
    }
    
    /**
     * 判断是否需要记录日志
     * 
     * @param mapperId Mapper 方法全限定名
     * @return 是否需要记录日志
     */
    public boolean shouldLog(String mapperId) {
        if (mapperId == null) {
            return false;
        }
        List<String> whitelist = sqlLogProperties.getWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        
        for (String pattern : whitelist) {
            if (pathMatcher.match(pattern, mapperId)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 准备 SQL 执行信息（执行前）
     * 收集执行前的信息，包括 SQL 语句、参数等
     * 
     * @param ms MappedStatement
     * @param parameter 参数对象
     * @param operationType 操作类型（query 或 update）
     * @return SQL 执行信息对象
     */
    public SqlExecutionInfo prepareExecutionInfo(MappedStatement ms, Object parameter, String operationType) {
        SqlExecutionInfo info = new SqlExecutionInfo();
        info.setMapperId(ms.getId());
        info.setOperationType(operationType);
        info.setStartTime(System.currentTimeMillis());
        
        // 收集应用和实例信息
        try {
            info.setApplicationName(ApplicationInstanceInfo.getApplicationName());
            info.setInstanceId(ApplicationInstanceInfo.getInstanceId());
            info.setHostname(ApplicationInstanceInfo.getHostname());
            info.setIp(ApplicationInstanceInfo.getIp());
            info.setPort(ApplicationInstanceInfo.getPort());
        } catch (Exception e) {
            log.warn("收集应用实例信息失败", e);
        }
        
        try {
            BoundSql boundSql = ms.getBoundSql(parameter);
            info.setOriginalSql(boundSql.getSql());
            info.setParameters(ReadableSqlUtil.getParameterValues(boundSql));
            info.setParametersFormatted(ReadableSqlUtil.formatParametersForOfficial(boundSql));
            
            // 无论什么格式，都提前准备可读 SQL，便于后续统一处理
            info.setReadableSql(ReadableSqlUtil.getReadableSql(boundSql));
        } catch (Exception e) {
            log.error("准备 SQL 执行信息失败", e);
            info.setErrorMessage("准备 SQL 执行信息失败: " + e.getMessage());
        }
        
        return info;
    }
    
    /**
     * 完成 SQL 执行信息（执行后）
     * 
     * @param info SQL 执行信息对象
     * @param result 执行结果
     * @param stopWatch 计时器
     * @param boundSql BoundSql 对象（用于格式化结果）
     */
    public void completeExecutionInfo(SqlExecutionInfo info, Object result, StopWatch stopWatch, BoundSql boundSql) {
        info.setEndTime(System.currentTimeMillis());
        info.setExecutionTime(stopWatch.getTotal(TimeUnit.MILLISECONDS));
        info.setSuccess(true);
        
        if ("query".equals(info.getOperationType())) {
            info.setResult(result);
            
            // 格式化查询结果信息
            try {
                Boolean showColumns = sqlLogProperties.getShowColumns();
                Boolean showRows = sqlLogProperties.getShowRows();
                Boolean showTotal = sqlLogProperties.getShowTotal();
                
                if ((showColumns != null && showColumns) || 
                    (showRows != null && showRows) || 
                    (showTotal != null && showTotal)) {
                    
                    List<String> resultLines = ReadableSqlUtil.formatResultForOfficial(
                        result, 
                        showColumns != null && showColumns,
                        showRows != null && showRows,
                        showTotal != null && showTotal
                    );
                    
                    // 提取列信息和行数
                    if (showColumns != null && showColumns && !resultLines.isEmpty()) {
                        String columnsLine = resultLines.stream()
                            .filter(line -> line.startsWith("<==    Columns:"))
                            .findFirst()
                            .orElse(null);
                        if (columnsLine != null) {
                            String columnsStr = columnsLine.replace("<==    Columns: ", "");
                            info.setColumns(java.util.Arrays.asList(columnsStr.split(", ")));
                        }
                    }
                    
                    if (showTotal != null && showTotal) {
                        String totalLine = resultLines.stream()
                            .filter(line -> line.startsWith("<==      Total:"))
                            .findFirst()
                            .orElse(null);
                        if (totalLine != null) {
                            String totalStr = totalLine.replace("<==      Total: ", "");
                            info.setTotalRows(Integer.parseInt(totalStr));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("格式化查询结果失败", e);
            }
        }
    }
    
    /**
     * 记录执行异常
     * 
     * @param info SQL 执行信息对象
     * @param e 异常
     */
    public void recordException(SqlExecutionInfo info, Throwable e) {
        info.setSuccess(false);
        info.setErrorMessage(e.getMessage());
        info.setEndTime(System.currentTimeMillis());
        if (info.getStartTime() != null) {
            info.setExecutionTime(System.currentTimeMillis() - info.getStartTime());
        }
    }
    
    /**
     * 处理 SQL 执行信息（统一进行打印和推送）
     * 先收集完整信息，然后统一委托给 SqlAuditService 处理打印和推送
     * 本地打印和推送可以同时存在，不冲突
     * 
     * @param info SQL 执行信息对象
     */
    public void handleSqlExecutionInfo(SqlExecutionInfo info) {
        if (info == null || !shouldLog(info.getMapperId())) {
            return;
        }
        
        try {
            // 统一委托给审计服务处理打印和推送
            // 审计服务会根据配置同时执行本地打印和其他推送方式
            sqlAuditService.push(info);
        } catch (Exception e) {
            log.error("处理 SQL 执行信息失败", e);
        }
    }
}
