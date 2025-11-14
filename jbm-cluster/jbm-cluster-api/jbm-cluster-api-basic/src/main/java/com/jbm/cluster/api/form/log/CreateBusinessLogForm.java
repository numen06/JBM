package com.jbm.cluster.api.form.log;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 创建业务日志表单
 * 
 * 支持两种使用方式：
 * 1. 原有格式 - 设置 module、operation、userId 等字段（用于传统业务日志）
 * 2. 集成格式 - 设置 businessType、businessId、source 等字段（用于集成模块）
 * 
 * @author wesley
 */
@Data
@ApiModel(value = "创建业务日志表单")
public class CreateBusinessLogForm {
    
    // ==================== 原有字段 ====================
    
    @ApiModelProperty(value = "业务模块名称")
    private String module;
    
    @ApiModelProperty(value = "业务操作类型")
    private String operation;
    
    @ApiModelProperty(value = "用户ID")
    private String userId;
    
    @ApiModelProperty(value = "用户名")
    private String username;
    
    @ApiModelProperty(value = "初始日志内容", notes = "可以包含初始的日志内容，支持多行")
    private String content;
    
    @ApiModelProperty(value = "是否自动添加时间戳前缀", notes = "true: 每行自动添加 [时间戳] 前缀")
    private Boolean autoTimestamp = false;
    
    @ApiModelProperty(value = "过期时间（天数）", notes = "默认30天")
    private Integer expireDays = 30;
    
    @ApiModelProperty(value = "请求IP")
    private String requestIp;
    
    @ApiModelProperty(value = "追踪ID")
    private String traceId;
    
    @ApiModelProperty(value = "备注")
    private String remark;
    
    // ==================== 集成模块字段 ====================
    
    @ApiModelProperty(value = "业务类型", notes = "如：ORDER、PAYMENT、USER_IMPORT 等，用于集成模块")
    private String businessType;
    
    @ApiModelProperty(value = "业务ID", notes = "如：订单号、支付流水号、任务ID 等，用于集成模块")
    private String businessId;
    
    @ApiModelProperty(value = "日志来源", notes = "如：order-service、payment-service 等，用于集成模块")
    private String source;
}

