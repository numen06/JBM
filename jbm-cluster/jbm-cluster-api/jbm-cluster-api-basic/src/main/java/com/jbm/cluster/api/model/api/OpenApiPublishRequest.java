package com.jbm.cluster.api.model.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("OpenAPI 发布请求")
public class OpenApiPublishRequest {

    @ApiModelProperty("文档键")
    private String docKey;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("版本")
    private String version;

    @ApiModelProperty("发布说明")
    private String publishedSummary;

    @ApiModelProperty("选择模式")
    private String selectionMode;

    @ApiModelProperty("服务 ID 列表")
    private List<String> serviceIds;

    @ApiModelProperty("操作 ID 列表")
    private List<Long> operationIds;
}
