package com.jbm.framework.dao.mybatis.sqlInjector;

import cn.hutool.core.date.StopWatch;
import cn.hutool.db.sql.SqlUtil;
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
        String readableSql = null;
        if (printLog(ms.getId())) {
            Object parameter = invocation.getArgs()[1];
            BoundSql boundSql = ms.getBoundSql(parameter);
            readableSql = ReadableSqlUtil.getReadableSql(boundSql);
        }
        try {
            return invocation.proceed();
        } finally {
            stopWatch.stop();
            if (readableSql != null) {
                log.info("[SQL Run Time : {} ms ],[SQL Mapper : {}] \n {}", stopWatch.getTotal(TimeUnit.MILLISECONDS), ms.getId(), readableSql);
            }
            MappedStatementHolder.clear();
        }
        // ✅ 必须清理！防内存泄漏
    }

    public boolean printLog(String msId) {
        // ✅ 从当前线程栈中提取 MappedStatement.getId()
        if (msId == null || sqlLogProperties.getWhitelist().isEmpty()) {
            return false;
        }
//        logger.info("msId: {}", msId);
        AntPathMatcher matcher = new AntPathMatcher();
        for (String pattern : sqlLogProperties.getWhitelist()) {
            if (matcher.match(pattern, msId)) {
                return true;
            }
        }
        return true;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
