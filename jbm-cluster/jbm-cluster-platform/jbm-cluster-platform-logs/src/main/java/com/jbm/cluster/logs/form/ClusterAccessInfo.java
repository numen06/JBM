package com.jbm.cluster.logs.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(value = "访问信息")
public class ClusterAccessInfo {

    @ApiModelProperty(value = "统计时间")
    private Date time;
    @ApiModelProperty(value = "访问总数")
    private Long total;
    @ApiModelProperty(value = "访问总数")
    private Long today;
    @ApiModelProperty(value = "每秒请求数")
    private Long maxQps;
//    @ApiModelProperty(value = "访客数")
//    private Long visitors;
//    @ApiModelProperty(value = "访问IP数量")
//    private Long ips;
}
