package com.jbm.cluster.api.entitys.log;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 业务日志阶段快照
 */
@Data
@ApiModel(value = "业务日志阶段快照")
public class BusinessLogStageSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "业务日志ID")
    private String logId;

    @ApiModelProperty(value = "阶段数量")
    private Integer stageCount;

    @ApiModelProperty(value = "整体进度（0-100）")
    private Integer overallProgress;

    @ApiModelProperty(value = "整体状态：WAITING、RUNNING、DONE、FAILED")
    private String overallStatus;

    @ApiModelProperty(value = "当前阶段序号")
    private Integer activeStageIndex;

    @ApiModelProperty(value = "当前阶段名称")
    private String activeStageName;

    @ApiModelProperty(value = "最近更新时间")
    private Date updateTime;

    @ApiModelProperty(value = "阶段列表快照")
    private List<BusinessLogStageItem> stages;

    @ApiModelProperty(value = "内部版本号，用于SSE增量推送")
    private Long version;
}


