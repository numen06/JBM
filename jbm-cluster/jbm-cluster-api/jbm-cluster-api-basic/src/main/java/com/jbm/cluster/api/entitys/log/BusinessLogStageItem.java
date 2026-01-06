package com.jbm.cluster.api.entitys.log;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务日志阶段信息
 * 既可用于初始化阶段配置，也可用于阶段快照响应
 */
@Data
@ApiModel(value = "业务日志阶段信息")
public class BusinessLogStageItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "阶段编码，未传则自动生成 stage-{order}")
    private String stageCode;

    @ApiModelProperty(value = "阶段名称")
    private String stageName;

    @ApiModelProperty(value = "阶段顺序（从1开始）")
    private Integer orderIndex;

    @ApiModelProperty(value = "阶段进度（0-100）")
    private Integer progress;

    @ApiModelProperty(value = "阶段状态：WAITING、RUNNING、DONE、FAILED")
    private String status;

    @ApiModelProperty(value = "阶段提示信息")
    private String message;

    @ApiModelProperty(value = "最近更新时间")
    private Date updateTime;
}


