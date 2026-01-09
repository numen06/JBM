package com.jbm.framework.dao;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SQL自动执行配置属性
 * 
 * @author wesley
 */
@Data
@ConfigurationProperties(prefix = "jbm.sql.auto-execute")
public class SqlAutoExecuteProperties {
    
    /**
     * 是否启用SQL自动执行（默认true）
     */
    private Boolean enabled = true;
    
    /**
     * 指定数据源Bean名称（可选，默认使用@Primary数据源）
     * 如果未指定，将优先使用Spring容器中@Primary标注的DataSource
     */
    private String datasourceBeanName;
    
    /**
     * 模块名称（可选，用于标识SQL文件来源）
     * 如果不指定，将从SQL文件的classpath路径中自动提取
     * 建议设置为固定的模块标识，避免因不同配置导致重复执行
     */
    private String moduleName;
}
