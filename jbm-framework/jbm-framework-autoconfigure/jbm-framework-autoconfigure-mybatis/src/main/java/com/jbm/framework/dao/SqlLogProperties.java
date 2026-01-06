package com.jbm.framework.dao;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author wesley
 */
@Data
@ConfigurationProperties(prefix = "sql-log")
public class SqlLogProperties {
    /**
     * SQL日志白名单，匹配的mapper方法会输出日志
     */
    private List<String> whitelist;

    /**
     * 日志格式类型：MERGED（合并格式，SQL和参数合并）或 OFFICIAL（官方格式，分别显示Preparing和Parameters）
     * 默认 MERGED，保持向后兼容
     */
    private SqlLogFormat format = SqlLogFormat.MERGED;

    /**
     * 是否显示列信息（仅official格式时有效）
     * 默认 false
     */
    private Boolean showColumns = false;

    /**
     * 是否显示行数据（仅official格式时有效）
     * 默认 false
     */
    private Boolean showRows = false;

    /**
     * 是否显示总数（仅official格式时有效）
     * 默认 false
     */
    private Boolean showTotal = false;
}