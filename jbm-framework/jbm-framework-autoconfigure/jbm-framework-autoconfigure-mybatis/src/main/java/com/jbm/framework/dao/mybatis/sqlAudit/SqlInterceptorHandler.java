package com.jbm.framework.dao.mybatis.sqlAudit;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.dao.SqlLogMode;
import com.jbm.framework.dao.SqlLogProperties;
import com.jbm.framework.dao.mybatis.sqlInjector.ReadableSqlUtil;
import jbm.framework.spring.ApplicationInstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.springframework.util.AntPathMatcher;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
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
        // 打印启动配置信息
        printStartupInfo();
    }
    
    /**
     * 打印启动配置信息
     */
    private void printStartupInfo() {
        try {
            // 检查是否显示 Banner
            Boolean showBanner = sqlLogProperties.getShowBanner();
            if (showBanner != null && !showBanner) {
                return; // 不显示 Banner
            }
            
            // 从资源文件读取 Banner 模板
            String bannerTemplate = loadBannerFromResource();
            if (StrUtil.isNotBlank(bannerTemplate)) {
                // 准备模板变量
                Map<String, String> variables = buildBannerVariables();
                // 替换模板变量
                String bannerContent = replaceTemplateVariables(bannerTemplate, variables);
                // 使用 System.out 直接输出，整块打印（类似 Spring Boot banner）
                System.out.println(bannerContent);
            }
        } catch (Exception e) {
            log.warn("打印启动配置信息失败", e);
        }
    }
    
    /**
     * 从资源文件加载 Banner
     * 
     * @return Banner 模板内容，如果文件不存在返回 null
     */
    private String loadBannerFromResource() {
        String bannerLocation = sqlLogProperties.getBannerLocation();
        if (StrUtil.isBlank(bannerLocation)) {
            bannerLocation = "sql-audit-banner.txt"; // 默认文件
        }
        
        try {
            // 从 classpath 读取文件
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(bannerLocation);
            if (inputStream != null) {
                String content = IoUtil.read(inputStream, StandardCharsets.UTF_8);
                IoUtil.close(inputStream);
                return content;
            }
        } catch (Exception e) {
            log.debug("无法加载 Banner 文件: {}", bannerLocation, e);
        }
        
        return null;
    }
    
    /**
     * 构建 Banner 模板变量
     */
    private Map<String, String> buildBannerVariables() {
        Map<String, String> variables = new HashMap<>();
        
        // 模式配置（简化显示）
        SqlLogMode mode = sqlLogProperties.getMode();
        if (mode == null) {
            mode = SqlLogMode.WHITELIST;
        }
        variables.put("mode", mode == SqlLogMode.NORMAL ? "普通模式" : "白名单模式");
        
        // 慢查询配置（简化显示）
        SqlLogProperties.SlowQueryProperties slowQuery = sqlLogProperties.getSlowQuery();
        if (slowQuery != null && (slowQuery.getEnabled() == null || slowQuery.getEnabled())) {
            Long threshold = slowQuery.getThreshold() != null ? slowQuery.getThreshold() : 3000L;
            variables.put("slowQuery", threshold + "ms");
        } else {
            variables.put("slowQuery", "关闭");
        }
        
        // 过滤配置（简化显示）
        Boolean filter = sqlLogProperties.getFilter();
        variables.put("filter", (filter != null && filter) ? "开" : "关");
        
        // 审计配置（简化显示）
        SqlLogProperties.SqlAuditProperties audit = sqlLogProperties.getAudit();
        variables.put("audit", (audit != null && (audit.getEnabled() == null || audit.getEnabled())) ? "开" : "关");
        
        return variables;
    }
    
    /**
     * 替换模板变量
     * 
     * @param template 模板内容
     * @param variables 变量映射
     * @return 替换后的内容
     */
    private String replaceTemplateVariables(String template, Map<String, String> variables) {
        if (StrUtil.isBlank(template)) {
            return template;
        }
        
        Pattern variablePattern = Pattern.compile("\\$\\{([^}]+)\\}");
        String[] templateLines = template.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (String templateLine : templateLines) {
            // 检查这一行是否包含变量
            Matcher varMatcher = variablePattern.matcher(templateLine);
            
            if (varMatcher.find()) {
                // 包含变量，进行替换
                StringBuffer lineBuffer = new StringBuffer();
                varMatcher.reset();
                while (varMatcher.find()) {
                    String key = varMatcher.group(1);
                    String value = variables.getOrDefault(key, "");
                    varMatcher.appendReplacement(lineBuffer, Matcher.quoteReplacement(value));
                }
                varMatcher.appendTail(lineBuffer);
                
                String replacedLine = lineBuffer.toString();
                // 如果替换后整行只包含空白字符（空变量替换后的结果），则跳过这一行
                if (!replacedLine.trim().isEmpty()) {
                    result.append(replacedLine).append("\n");
                }
            } else {
                // 不包含变量，直接保留原行
                result.append(templateLine).append("\n");
            }
        }
        
        // 移除末尾多余的换行
        String finalResult = result.toString();
        if (finalResult.endsWith("\n")) {
            finalResult = finalResult.substring(0, finalResult.length() - 1);
        }
        
        return finalResult;
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
        
        // 根据模式决定是否记录
        SqlLogMode mode = sqlLogProperties.getMode();
        if (mode == null) {
            mode = SqlLogMode.WHITELIST; // 默认白名单模式
        }
        
        if (mode == SqlLogMode.NORMAL) {
            // 普通模式：记录所有 SQL（后续会通过过滤规则过滤）
            return true;
        } else {
            // 白名单模式：只记录白名单中匹配的 SQL
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
     */
    public void completeExecutionInfo(SqlExecutionInfo info, Object result, StopWatch stopWatch) {
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
        
        // 简化错误信息，避免过长
        String errorMessage = e.getMessage();
        if (errorMessage != null) {
            Integer maxLength = sqlLogProperties.getMaxErrorMessageLength();
            if (maxLength != null && maxLength > 0 && errorMessage.length() > maxLength) {
                errorMessage = errorMessage.substring(0, maxLength) + "...";
            }
            // 移除换行符，保持一行
            errorMessage = errorMessage.replaceAll("\\s+", " ").trim();
        }
        info.setErrorMessage(errorMessage);
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
        if (info == null) {
            return;
        }
        
        // 检查是否是慢查询（慢查询不受白名单限制，始终记录）
        boolean isSlowQuery = isSlowQuery(info);
        
        // 如果不是慢查询，则需要检查白名单
        if (!isSlowQuery && !shouldLog(info.getMapperId())) {
            return;
        }
        
        // 如果 SQL 执行失败，根据配置决定是否记录（默认记录，但会简化输出）
        if (info.getSuccess() != null && !info.getSuccess()) {
            Boolean logFailedSql = sqlLogProperties.getLogFailedSql();
            if (logFailedSql != null && !logFailedSql) {
                // 如果配置为不记录失败的 SQL，直接返回
                return;
            }
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
    
    /**
     * 检查是否是慢查询
     * 
     * @param info SQL 执行信息对象
     * @return 是否是慢查询
     */
    private boolean isSlowQuery(SqlExecutionInfo info) {
        // 检查慢查询配置是否启用
        SqlLogProperties.SlowQueryProperties slowQueryProps = sqlLogProperties.getSlowQuery();
        if (slowQueryProps == null || slowQueryProps.getEnabled() == null || !slowQueryProps.getEnabled()) {
            return false;
        }
        
        // 检查是否是慢查询
        return info.getSlowQuery() != null && info.getSlowQuery();
    }
}
