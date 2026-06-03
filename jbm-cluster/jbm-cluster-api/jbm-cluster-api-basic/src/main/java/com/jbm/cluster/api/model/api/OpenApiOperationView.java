package com.jbm.cluster.api.model.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("OpenAPI 接口索引视图")
public class OpenApiOperationView {

    @ApiModelProperty("操作 ID")
    private Long operationId;

    @ApiModelProperty("服务 ID")
    private String serviceId;

    @ApiModelProperty("HTTP 方法")
    private String method;

    @ApiModelProperty("路径")
    private String path;

    @ApiModelProperty("标签")
    private String tag;

    @ApiModelProperty("摘要")
    private String summary;

    @ApiModelProperty("关联 apiId")
    private Long apiId;

    @ApiModelProperty("apiCode")
    private String apiCode;

    @ApiModelProperty("是否开放")
    private Integer isOpen;

    @ApiModelProperty("是否认证")
    private Boolean isAuth;

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("是否已关联")
    private Boolean linked;

    @ApiModelProperty("同步状态")
    private String syncState;

    @ApiModelProperty("是否废弃")
    private Integer deprecated;
}
