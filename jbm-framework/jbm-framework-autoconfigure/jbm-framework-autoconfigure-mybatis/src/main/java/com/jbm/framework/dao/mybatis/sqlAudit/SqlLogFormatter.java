package com.jbm.framework.dao.mybatis.sqlAudit;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import jbm.framework.spring.config.SpringContextHolder;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 日志格式化工具类
 * 支持类似 p6spy 的格式化字符串
 * 
 * @author wesley
 */
public class SqlLogFormatter {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%\\(([^)]+)\\)");
    private static volatile String cachedDataSourceName;
    
    /**
     * 格式化 SQL 日志
     * 
     * @param format 格式化字符串，例如：%(currentTime) | DS: %(dataSource) | took %(executionTime)ms | %(sql)
     * @param executionInfo SQL 执行信息
     * @return 格式化后的日志字符串
     */
    public static String format(String format, SqlExecutionInfo executionInfo) {
        if (StrUtil.isBlank(format) || executionInfo == null) {
            return "";
        }
        
        // 准备变量映射
        Map<String, String> variables = buildVariables(executionInfo);
        
        // 替换占位符
        String result = format;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(format);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(0); // %(key)
            String key = matcher.group(1); // key
            String value = variables.getOrDefault(key, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 构建变量映射
     */
    private static Map<String, String> buildVariables(SqlExecutionInfo executionInfo) {
        Map<String, String> variables = new HashMap<>();
        
        // 当前时间
        variables.put("currentTime", formatCurrentTime());
        
        // 数据源（如果可用）
        String dataSource = getDataSourceName();
        variables.put("dataSource", dataSource);
        
        // 执行时间
        if (executionInfo.getExecutionTime() != null) {
            variables.put("executionTime", String.valueOf(executionInfo.getExecutionTime()));
        } else {
            variables.put("executionTime", "0");
        }
        
        // SQL 语句（优先使用可读 SQL，如果没有则使用原始 SQL）
        String sql = null;
        if (StrUtil.isNotBlank(executionInfo.getReadableSql())) {
            sql = executionInfo.getReadableSql();
        } else if (StrUtil.isNotBlank(executionInfo.getOriginalSql())) {
            sql = executionInfo.getOriginalSql();
        }
        
        // 确保 SQL 语句末尾有分号
        if (StrUtil.isNotBlank(sql)) {
            sql = sql.trim();
            if (!sql.endsWith(";")) {
                sql = sql + ";";
            }
            variables.put("sql", sql);
        } else {
            variables.put("sql", "");
        }
        
        // Mapper ID
        if (StrUtil.isNotBlank(executionInfo.getMapperId())) {
            variables.put("mapperId", executionInfo.getMapperId());
        } else {
            variables.put("mapperId", "");
        }
        
        // 操作类型
        if (StrUtil.isNotBlank(executionInfo.getOperationType())) {
            variables.put("operationType", executionInfo.getOperationType());
        } else {
            variables.put("operationType", "");
        }
        
        // 应用名称
        if (StrUtil.isNotBlank(executionInfo.getApplicationName())) {
            variables.put("applicationName", executionInfo.getApplicationName());
        } else {
            variables.put("applicationName", "");
        }
        
        // 实例ID
        if (StrUtil.isNotBlank(executionInfo.getInstanceId())) {
            variables.put("instanceId", executionInfo.getInstanceId());
        } else {
            variables.put("instanceId", "");
        }
        
        // 慢查询标记
        if (executionInfo.getSlowQuery() != null && executionInfo.getSlowQuery()) {
            variables.put("slowQuery", "⚠️ [慢查询]");
        } else {
            variables.put("slowQuery", "");
        }
        
        // 执行结果
        if (executionInfo.getSuccess() != null && executionInfo.getSuccess()) {
            variables.put("result", "SUCCESS");
        } else {
            variables.put("result", "FAILED");
        }
        
        return variables;
    }
    
    /**
     * 格式化当前时间
     */
    private static String formatCurrentTime() {
        return DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");
    }
    
    /**
     * 获取数据源名称
     */
    private static String getDataSourceName() {
        if (cachedDataSourceName != null) {
            return cachedDataSourceName;
        }
        
        synchronized (SqlLogFormatter.class) {
            if (cachedDataSourceName != null) {
                return cachedDataSourceName;
            }
            
            try {
                ApplicationContext context = SpringContextHolder.getApplicationContext();
                if (context != null) {
                    // 尝试获取数据源 Bean 名称
                    try {
                        DataSource dataSource = context.getBean(DataSource.class);
                        String[] beanNames = context.getBeanNamesForType(DataSource.class);
                        if (beanNames.length > 0) {
                            cachedDataSourceName = beanNames[0];
                            return cachedDataSourceName;
                        }
                    } catch (Exception e) {
                        // 多个数据源时，尝试获取主数据源
                    }
                    
                    // 尝试从配置获取数据源名称
                    try {
                        com.jbm.framework.dao.JdbcDataSourceProperties props = 
                            context.getBean(com.jbm.framework.dao.JdbcDataSourceProperties.class);
                        if (props != null && StrUtil.isNotBlank(props.getName())) {
                            cachedDataSourceName = props.getName();
                            return cachedDataSourceName;
                        }
                    } catch (Exception e) {
                        // 忽略
                    }
                }
            } catch (Exception e) {
                // 忽略
            }
            
            cachedDataSourceName = "default";
            return cachedDataSourceName;
        }
    }
}
