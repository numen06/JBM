package com.jbm.framework.dao.expand;

import lombok.Data;

import java.util.Date;

/**
 * SQL执行记录实体
 * 
 * @author wesley
 */
@Data
public class SqlInitialize {

    /**
     * SQL文件路径（相对路径）
     */
    private String fileName;
    
    /**
     * SQL文件版本号（日期格式：8位数字，如20240101）
     */
    private String version;
    
    /**
     * 模块/应用名称（标识是哪个应用执行的）
     */
    private String moduleName;
    
    /**
     * SQL文件哈希值（用于检测文件是否被修改）
     */
    private String fileHash;
    
    /**
     * 执行状态：SUCCESS-成功, FAILED-失败
     */
    private String executeStatus;
    
    /**
     * 错误信息（执行失败时记录）
     */
    private String errorMessage;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long executionTime;
    
    /**
     * 执行时间
     */
    private Date createTime;
}
