package com.jbm.framework.dao.mybatis.sqlAudit;

import lombok.Data;

import java.util.List;

/**
 * SQL 执行信息封装类
 * 用于记录 SQL 执行的详细信息，便于后续日志审计和平台推送
 * 
 * @author wesley
 */
@Data
public class SqlExecutionInfo {
    /**
     * Mapper 方法全限定名（如：com.xxx.mapper.UserMapper.selectById）
     */
    private String mapperId;
    
    /**
     * SQL 语句（原始 SQL，包含 ? 占位符）
     */
    private String originalSql;
    
    /**
     * 可读的 SQL 语句（参数已替换）
     */
    private String readableSql;
    
    /**
     * SQL 参数列表
     */
    private List<Object> parameters;
    
    /**
     * SQL 参数格式化后的字符串（官方格式）
     */
    private String parametersFormatted;
    
    /**
     * 操作类型：query（查询）或 update（更新）
     */
    private String operationType;
    
    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;
    
    /**
     * 查询结果（仅查询操作）
     */
    private Object result;
    
    /**
     * 查询结果列信息（仅查询操作）
     */
    private List<String> columns;
    
    /**
     * 查询结果行数（仅查询操作）
     */
    private Integer totalRows;
    
    /**
     * 是否执行成功
     */
    private Boolean success;
    
    /**
     * 异常信息（如果执行失败）
     */
    private String errorMessage;
    
    /**
     * 执行开始时间戳
     */
    private Long startTime;
    
    /**
     * 执行结束时间戳
     */
    private Long endTime;
    
    /**
     * 应用名称
     */
    private String applicationName;
    
    /**
     * 实例ID（用于区分同一应用的不同实例）
     */
    private String instanceId;
    
    /**
     * 主机名
     */
    private String hostname;
    
    /**
     * IP地址
     */
    private String ip;
    
    /**
     * 端口号
     */
    private String port;
    
    /**
     * 是否为慢查询
     */
    private Boolean slowQuery;
    
    /**
     * 慢查询阈值（毫秒）
     */
    private Long slowQueryThreshold;
}
