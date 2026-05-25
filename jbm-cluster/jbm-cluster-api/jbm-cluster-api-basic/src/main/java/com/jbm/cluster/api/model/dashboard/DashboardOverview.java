package com.jbm.cluster.api.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 仪表盘聚合概览
 */
@Data
@ApiModel("仪表盘聚合概览")
public class DashboardOverview implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("当前身份")
    private DashboardIdentity identity;

    @ApiModelProperty("工作区分区可见性")
    private DashboardSection sections;

    @ApiModelProperty("按权限裁剪后的指标")
    private DashboardMetric metrics;

    @ApiModelProperty("治理风险提示")
    private List<DashboardRisk> risks = new ArrayList<>();
}
