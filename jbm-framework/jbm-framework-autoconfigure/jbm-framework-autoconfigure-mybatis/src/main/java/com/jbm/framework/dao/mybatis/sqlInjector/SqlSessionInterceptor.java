package com.jbm.framework.dao.mybatis.sqlInjector;

import cn.hutool.core.date.StopWatch;
import com.jbm.framework.dao.SqlLogProperties;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlExecutionInfo;
import com.jbm.framework.dao.mybatis.sqlAudit.SqlInterceptorHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

/**
 * SQL 会话拦截器
 * 负责拦截 MyBatis 的 SQL 执行，委托给 SqlInterceptorHandler 处理
 * 
 * @author wesley
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
@Slf4j
public class SqlSessionInterceptor implements Interceptor {
    
    private final SqlInterceptorHandler sqlInterceptorHandler;
    private final SqlLogProperties sqlLogProperties;

    public SqlSessionInterceptor(SqlLogProperties sqlLogProperties) {
        this.sqlLogProperties = sqlLogProperties;
        this.sqlInterceptorHandler = new SqlInterceptorHandler(sqlLogProperties);
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
        
        String operationType = "query".equals(invocation.getMethod().getName()) ? "query" : "update";
        Object parameter = args[1];
        SqlExecutionInfo executionInfo = null;
        
        // 判断是否需要记录日志（白名单检查）
        boolean shouldLog = sqlInterceptorHandler.shouldLog(ms.getId());
        
        // 检查慢查询功能是否启用（慢查询需要绕过白名单检查）
        boolean slowQueryEnabled = isSlowQueryEnabled();
        
        // 准备 SQL 执行信息（执行前收集基础信息）
        // 如果满足以下任一条件，都需要准备执行信息：
        // 1. 通过白名单检查（shouldLog = true）
        // 2. 慢查询功能启用（需要检测慢查询，即使不在白名单中）
        if (shouldLog || slowQueryEnabled) {
            try {
                executionInfo = sqlInterceptorHandler.prepareExecutionInfo(ms, parameter, operationType);
            } catch (Exception e) {
                log.error("准备 SQL 执行信息失败", e);
            }
        }
        
        Object result = null;
        try {
            result = invocation.proceed();
            return result;
        } catch (Throwable e) {
            // 记录异常信息
            if (executionInfo != null) {
                sqlInterceptorHandler.recordException(executionInfo, e);
            }
            throw e;
        } finally {
            stopWatch.stop();
            
            if (executionInfo != null && (shouldLog || slowQueryEnabled)) {
                try {
                    sqlInterceptorHandler.completeExecutionInfo(executionInfo, result, stopWatch);
                    sqlInterceptorHandler.handleSqlExecutionInfo(executionInfo);
                } catch (Exception e) {
                    log.error("处理 SQL 执行信息失败", e);
                }
            }
            
            // ✅ 必须清理！防内存泄漏
            MappedStatementHolder.clear();
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
    
    /**
     * 检查慢查询功能是否启用
     * 
     * @return 是否启用慢查询检测
     */
    private boolean isSlowQueryEnabled() {
        if (sqlLogProperties == null) {
            return false;
        }
        SqlLogProperties.SlowQueryProperties slowQuery = sqlLogProperties.getSlowQuery();
        if (slowQuery == null) {
            return false;
        }
        // 如果 enabled 为 null，默认启用
        return slowQuery.getEnabled() == null || slowQuery.getEnabled();
    }
}
