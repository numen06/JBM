package com.jbm.cluster.api.model.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("OpenAPI 同步结果")
public class OpenApiSyncResult {

    @ApiModelProperty("服务 ID")
    private String serviceId;

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

    @ApiModelProperty("内容哈希")
    private String sourceHash;

    @ApiModelProperty("同步时间")
    private Date syncTime;
}
