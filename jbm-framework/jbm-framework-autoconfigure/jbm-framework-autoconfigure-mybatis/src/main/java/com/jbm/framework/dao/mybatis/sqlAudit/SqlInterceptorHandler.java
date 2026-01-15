package com.jbm.framework.dao.mybatis.sqlAudit;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.dao.SqlLogProperties;
import com.jbm.framework.dao.mybatis.sqlInjector.ReadableSqlUtil;
import jbm.framework.spring.ApplicationInstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * SQL 拦截处理器
 * 负责收集 SQL 执行信息、格式化日志输出，并为后续日志审计和平台推送提供接口
 * 
 * @author wesley
 */
@Slf4j
public class SqlInterceptorHandler {

    private final SqlLogProperties sqlLogProperties;
    private final AntPathMatcher pathMatcher;
    private final SqlAuditService sqlAuditService;
    private Pattern excludePattern;
    
    public SqlInterceptorHandler(SqlLogProperties sqlLogProperties) {
        this.sqlLogProperties = sqlLogProperties;
        this.pathMatcher = new AntPathMatcher();
        // 初始化审计服务
        this.sqlAuditService = new SqlAuditService(sqlLogProperties);
        // 初始化过滤规则
        initializeExcludePattern();
    }
    
    /**
     * 初始化排除规则正则表达式
     */
    private void initializeExcludePattern() {
        if (sqlLogProperties.getFilter() != null && sqlLogProperties.getFilter()) {
            String exclude = sqlLogProperties.getExclude();
            if (StrUtil.isNotBlank(exclude)) {
                try {
                    // 将多个规则用 | 分隔，编译成正则表达式
                    this.excludePattern = Pattern.compile(exclude, Pattern.CASE_INSENSITIVE);
                } catch (Exception e) {
                    log.warn("SQL 过滤规则编译失败，将不使用过滤功能: {}", exclude, e);
                    this.excludePattern = null;
                }
            }
        }
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
        
        // 判断是否为慢查询
        SqlLogProperties.SlowQueryProperties slowQueryProps = sqlLogProperties.getSlowQuery();
        if (slowQueryProps != null && (slowQueryProps.getEnabled() == null || slowQueryProps.getEnabled())) {
            Long threshold = slowQueryProps.getThreshold() != null ? slowQueryProps.getThreshold() : 3000L;
            info.setSlowQueryThreshold(threshold);
            
            if (info.getExecutionTime() != null && info.getExecutionTime() >= threshold) {
                info.setSlowQuery(true);
            } else {
                info.setSlowQuery(false);
            }
        } else {
            info.setSlowQuery(false);
        }
        
        if ("query".equals(info.getOperationType())) {
            info.setResult(result);
            // 不再提取列信息和行数，统一使用格式化字符串一行打印，避免内容过多
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
     * 检查 SQL 是否应该被过滤掉
     * 
     * @param sql SQL 语句
     * @return true 表示应该过滤掉，false 表示不过滤
     */
    public boolean shouldFilter(String sql) {
        // 如果未启用过滤，不过滤
        if (sqlLogProperties.getFilter() == null || !sqlLogProperties.getFilter()) {
            return false;
        }
        
        // 如果没有排除规则，不过滤
        if (excludePattern == null) {
            return false;
        }
        
        // 如果 SQL 为空，不过滤
        if (StrUtil.isBlank(sql)) {
            return false;
        }
        
        // 检查 SQL 是否匹配排除规则（使用 find 来匹配部分内容）
        try {
            String trimmedSql = sql.trim();
            // 如果 SQL 末尾有分号，先去掉分号再匹配（因为配置中可能包含分号）
            if (trimmedSql.endsWith(";")) {
                trimmedSql = trimmedSql.substring(0, trimmedSql.length() - 1).trim();
            }
            return excludePattern.matcher(trimmedSql).find();
        } catch (Exception e) {
            log.warn("SQL 过滤检查失败", e);
            return false;
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
        
        // 检查是否需要过滤 SQL
        String sql = info.getReadableSql();
        if (StrUtil.isBlank(sql)) {
            sql = info.getOriginalSql();
        }
        if (shouldFilter(sql)) {
            // 如果应该过滤，直接返回，不记录日志
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
