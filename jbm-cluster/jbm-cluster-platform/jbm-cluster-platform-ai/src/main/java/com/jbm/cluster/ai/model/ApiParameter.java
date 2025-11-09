package com.jbm.cluster.ai.model;

import lombok.Data;

/**
 * API 参数
 * @author wesley
 */
@Data
public class ApiParameter {
    /**
     * 参数名
     */
    private String name;
    
    /**
     * 参数类型 (query, path, body, header)
     */
    private String in;
    
    /**
     * 参数数据类型 (string, integer, boolean, object, array)
     */
    private String type;
    
    /**
     * 参数描述
     */
    private String description;
    
    /**
     * 是否必需
     */
    private boolean required;
    
    /**
     * 默认值
     */
    private Object defaultValue;
    
    /**
     * 示例值
     */
    private Object example;
}

