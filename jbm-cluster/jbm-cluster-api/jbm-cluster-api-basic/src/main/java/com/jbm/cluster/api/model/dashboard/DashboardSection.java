package com.jbm.cluster.api.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 仪表盘工作区分区可见性
 */
@Data
@ApiModel("仪表盘工作区分区")
public class DashboardSection implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("系统治理")
    private Boolean system;

    @ApiModelProperty("权限治理")
    private Boolean authority;

    @ApiModelProperty("API 治理")
    private Boolean api;

    @ApiModelProperty("网关治理")
    private Boolean gateway;

    @ApiModelProperty("开放平台")
    private Boolean developer;

    @ApiModelProperty("审计安全")
    private Boolean audit;
}
