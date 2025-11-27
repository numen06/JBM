package com.jbm.cluster.api.form.log;

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
    
    @ApiModelProperty(value = "追加的日志内容", required = true, notes = "支持多行内容，每行会自动追加换行符")
    @NotBlank(message = "追加的日志内容不能为空")
    private String content;
    
    @ApiModelProperty(value = "是否自动添加时间戳前缀", notes = "true: 每行自动添加 [时间戳] 前缀")
    private Boolean autoTimestamp = false;

    // ==================== 阶段元数据（可选） ====================

    @ApiModelProperty(value = "阶段编码")
    private String stageCode;

    @ApiModelProperty(value = "阶段名称")
    private String stageName;

    @ApiModelProperty(value = "阶段序号")
    private Integer stageIndex;

    @ApiModelProperty(value = "阶段进度（0-100）")
    private Integer stageProgress;

    @ApiModelProperty(value = "阶段状态：WAITING、RUNNING、DONE、FAILED")
    private String stageStatus;

    @ApiModelProperty(value = "阶段总数")
    private Integer stageCount;

    @ApiModelProperty(value = "整体进度（0-100）")
    private Integer overallProgress;

    @ApiModelProperty(value = "是否为阶段事件")
    private Boolean stageEvent = false;
}

