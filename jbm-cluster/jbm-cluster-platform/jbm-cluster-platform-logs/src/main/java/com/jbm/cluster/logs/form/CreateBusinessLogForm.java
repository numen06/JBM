package com.jbm.cluster.logs.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 创建业务日志表单
 * 
 * @author wesley
 */
@Data
@ApiModel(value = "创建业务日志表单")
public class CreateBusinessLogForm {
    
    @ApiModelProperty(value = "业务模块名称", required = true)
    @NotBlank(message = "业务模块名称不能为空")
    private String module;
    
    @ApiModelProperty(value = "业务操作类型", required = true)
    @NotBlank(message = "业务操作类型不能为空")
    private String operation;
    
    @ApiModelProperty(value = "用户ID")
    private String userId;
    
    @ApiModelProperty(value = "用户名")
    private String username;
    
    @ApiModelProperty(value = "初始日志内容（支持多行）", required = false, notes = "可以包含初始的日志内容，支持多行")
    private String content;
    
    @ApiModelProperty(value = "是否自动添加时间戳前缀", notes = "true: 每行自动添加 [时间戳] 前缀")
    private Boolean autoTimestamp = false;
    
    @ApiModelProperty(value = "过期时间（天数），默认30天")
    private Integer expireDays = 30;
    
    @ApiModelProperty(value = "请求IP")
    private String requestIp;
    
    @ApiModelProperty(value = "追踪ID")
    private String traceId;
    
    @ApiModelProperty(value = "备注")
    private String remark;
}

