package com.jbm.cluster.api.model.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@ApiModel("OpenAPI 文档源")
public class OpenApiSource {

    @ApiModelProperty("服务 ID")
    private String serviceId;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("spec 读取 URL（管理端）")
    private String url;

    @ApiModelProperty("同步状态")
    private String syncStatus;

    @ApiModelProperty("同步消息")
    private String syncMessage;

    @ApiModelProperty("接口总数")
    private Integer operationTotal;

    @ApiModelProperty("已关联资源数")
    private Integer linkedApiTotal;

    @ApiModelProperty("未关联资源数")
    private Integer unlinkedApiTotal;

    @ApiModelProperty("最近同步时间")
    private Date lastSyncTime;
}
