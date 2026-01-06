package com.jbm.cluster.api.form.log;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 更新业务日志阶段进度
 */
@Data
@ApiModel(value = "业务日志阶段进度更新表单")
public class BusinessLogStageUpdateForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "业务日志ID", required = true)
    @NotBlank(message = "logId不能为空")
    private String logId;

    @ApiModelProperty(value = "阶段编码，可与阶段序号二选一")
    private String stageCode;

    @ApiModelProperty(value = "阶段序号（从1开始），可与阶段编码二选一")
    private Integer stageIndex;

    @ApiModelProperty(value = "阶段名称（可选，用于补充或新建阶段）")
    private String stageName;

    @ApiModelProperty(value = "阶段进度（0-100）")
    private Integer progress;

    @ApiModelProperty(value = "阶段状态：WAITING、RUNNING、DONE、FAILED")
    private String status;

    @ApiModelProperty(value = "阶段提示信息")
    private String message;

    @ApiModelProperty(value = "整体进度（0-100，可选）")
    private Integer overallProgress;

    @ApiModelProperty(value = "是否自动追加日志描述，默认true")
    private Boolean appendLog = true;

    @ApiModelProperty(value = "指定追加的日志内容，未指定则根据阶段信息生成")
    private String content;

    @ApiModelProperty(value = "追加日志时是否自动加时间戳")
    private Boolean autoTimestamp = true;
}


