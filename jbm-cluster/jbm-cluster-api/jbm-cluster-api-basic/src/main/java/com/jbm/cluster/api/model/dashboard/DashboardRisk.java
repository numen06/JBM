package com.jbm.cluster.api.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 仪表盘治理风险提示
 */
@Data
@ApiModel("仪表盘风险提示")
public class DashboardRisk implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("级别：info / warning / error")
    private String level;

    @ApiModelProperty("提示标题")
    private String title;

    @ApiModelProperty("关联前端路由")
    private String target;

    @ApiModelProperty("风险编码")
    private String code;
}
