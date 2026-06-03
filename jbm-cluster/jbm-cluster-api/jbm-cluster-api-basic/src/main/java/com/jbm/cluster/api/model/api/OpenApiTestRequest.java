package com.jbm.cluster.api.model.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

@Data
@ApiModel("OpenAPI 测试请求")
public class OpenApiTestRequest {

    @ApiModelProperty("操作 ID")
    private Long operationId;

    @ApiModelProperty("服务 ID（与 path+method 二选一）")
    private String serviceId;

    @ApiModelProperty("路径")
    private String path;

    @ApiModelProperty("HTTP 方法")
    private String method;

    @ApiModelProperty("路径参数")
    private Map<String, String> pathParams;

    @ApiModelProperty("查询参数")
    private Map<String, String> queryParams;

    @ApiModelProperty("请求头")
    private Map<String, String> headers;

    @ApiModelProperty("请求体")
    private String body;

    @ApiModelProperty("写操作二次确认")
    private Boolean confirm;
}
