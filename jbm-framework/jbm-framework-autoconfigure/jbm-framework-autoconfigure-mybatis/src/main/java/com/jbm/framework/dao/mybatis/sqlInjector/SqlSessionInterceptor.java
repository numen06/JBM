package com.jbm.framework.dao.mybatis.sqlInjector;

import cn.hutool.core.date.StopWatch;
import com.jbm.framework.dao.SqlLogFormat;
import com.jbm.framework.dao.SqlLogProperties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author wesley
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class SqlSessionInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(SqlSessionInterceptor.class);


    private final SqlLogProperties sqlLogProperties;

    public SqlSessionInterceptor(SqlLogProperties sqlLogProperties) {
        this.sqlLogProperties = sqlLogProperties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        StopWatch stopWatch = new StopWatch();
        if (args == null || args.length == 0) {
            return invocation.proceed();
        }
        stopWatch.start();
        MappedStatement ms = (MappedStatement) args[0];
        // ✅ 关键：存入全限定方法名
        MappedStatementHolder.set(ms.getId());
        
        boolean shouldLog = printLog(ms.getId());
        SqlLogFormat format = sqlLogProperties.getFormat();
        if (format == null) {
            format = SqlLogFormat.MERGED; // 默认合并格式
        }
        
        BoundSql boundSql = null;
        boolean isQuery = "query".equals(invocation.getMethod().getName());
        String readableSql = null;
        
        // 在执行前输出 Preparing（官方格式或合并格式）
        if (shouldLog) {
            try {
                Object parameter = invocation.getArgs()[1];
                boundSql = ms.getBoundSql(parameter);
                
                if (SqlLogFormat.OFFICIAL == format) {
                    // 官方格式：分别输出 Preparing 和 Parameters
                    String sql = boundSql.getSql();
                    log.info("==>  Preparing: {}", sql.endsWith(";") ? sql : sql + ";");
                    
                    // 输出 Parameters
                    String parameters = ReadableSqlUtil.formatParametersForOfficial(boundSql);
                    if (!parameters.isEmpty()) {
                        log.info("==> Parameters: {}", parameters);
                    }
                } else if (SqlLogFormat.MERGED == format) {
                    // 合并格式：准备替换参数后的 SQL（在执行后输出，包含执行时间和mapper信息）
                    readableSql = ReadableSqlUtil.getReadableSql(boundSql);
                }
            } catch (Exception e) {
                log.error("获取SQL失败", e);
            }
        }
        
        Object result = null;
        try {
            result = invocation.proceed();
            return result;
        } finally {
            stopWatch.stop();
            
            if (shouldLog) {
                if (SqlLogFormat.OFFICIAL == format) {
                    // 官方格式：输出结果信息（如果配置了）
                    if (isQuery && boundSql != null) {
                        Boolean showColumns = sqlLogProperties.getShowColumns();
                        Boolean showRows = sqlLogProperties.getShowRows();
                        Boolean showTotal = sqlLogProperties.getShowTotal();
                        
                        if ((showColumns != null && showColumns) || 
                            (showRows != null && showRows) || 
                            (showTotal != null && showTotal)) {
                            try {
                                List<String> resultLines = ReadableSqlUtil.formatResultForOfficial(
                                    result, 
                                    showColumns != null && showColumns,
                                    showRows != null && showRows,
                                    showTotal != null && showTotal
                                );
                                for (String line : resultLines) {
                                    log.info(line);
                                }
                            } catch (Exception e) {
                                log.error("格式化查询结果失败", e);
                            }
                        }
                    }
                } else if (SqlLogFormat.MERGED == format) {
                    // 合并格式：输出包含执行时间和mapper信息的完整日志
                    if (readableSql != null) {
                        String sql = readableSql.endsWith(";") ? readableSql : readableSql + ";";
                        log.info("[SQL Run Time : {} ms ],[SQL Mapper : {}] \n {}", 
                                stopWatch.getTotal(TimeUnit.MILLISECONDS), ms.getId(), sql);
                    }
                }
            }
            
            MappedStatementHolder.clear();
        }
        // ✅ 必须清理！防内存泄漏
    }

    public boolean printLog(String msId) {
        // ✅ 从当前线程栈中提取 MappedStatement.getId()
        if (msId == null) {
            return false;
        }
        List<String> whitelist = sqlLogProperties.getWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
//        logger.info("msId: {}", msId);
        AntPathMatcher matcher = new AntPathMatcher();
        for (String pattern : whitelist) {
            if (matcher.match(pattern, msId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
