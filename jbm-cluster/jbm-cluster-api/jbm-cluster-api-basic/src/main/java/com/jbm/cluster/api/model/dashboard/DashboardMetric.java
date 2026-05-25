package com.jbm.cluster.api.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 仪表盘指标集合（仅返回当前用户有权查看的字段）
 */
@Data
@ApiModel("仪表盘指标")
public class DashboardMetric implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户总数")
    private Long usersTotal;

    @ApiModelProperty("在线用户数")
    private Long onlineUser;

    @ApiModelProperty("应用数")
    private Long appCount;

    @ApiModelProperty("组织数")
    private Long orgCount;

    @ApiModelProperty("角色数")
    private Long roleCount;

    @ApiModelProperty("权限资源数")
    private Long authorityResourceCount;

    @ApiModelProperty("API 资源数")
    private Long apiCount;

    @ApiModelProperty("API Key 数")
    private Long apiKeyCount;
}
