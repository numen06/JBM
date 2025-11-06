package com.jbm.cluster.logs.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 追加业务日志表单
 * 支持追加多行日志内容，类似文件追加写入
 * 
 * @author wesley
 */
@Data
@ApiModel(value = "追加业务日志表单")
public class AppendBusinessLogForm {
    
    @ApiModelProperty(value = "业务日志ID", required = true)
    @NotBlank(message = "业务日志ID不能为空")
    private String logId;
    
    @ApiModelProperty(value = "追加的日志内容（支持多行）", required = true, notes = "可以追加多行内容，每行会自动追加换行符")
    @NotBlank(message = "追加的日志内容不能为空")
    private String content;
    
    @ApiModelProperty(value = "是否自动添加时间戳前缀", notes = "true: 每行自动添加 [时间戳] 前缀")
    private Boolean autoTimestamp = false;
}

