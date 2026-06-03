package com.jbm.cluster.api.model.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@ApiModel("OpenAPI 同步请求")
public class OpenApiSyncRequest {

    @ApiModelProperty("指定服务 ID 列表，空则同步全部")
    private List<String> serviceIds;
}
