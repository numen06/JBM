package com.jbm.cluster.api.model.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@ApiModel("OpenAPI 导出请求")
public class OpenApiExportRequest {

    @ApiModelProperty("格式 JSON/MARKDOWN/HTML/YAML")
    private String format;

    @ApiModelProperty("选择模式 ALL/FILTERED/CHECKED/TAG")
    private String selectionMode;

    @ApiModelProperty("服务 ID 列表")
    private List<String> serviceIds;

    @ApiModelProperty("操作 ID 列表")
    private List<Long> operationIds;

    @ApiModelProperty("筛选条件")
    private Map<String, Object> filters;

    @ApiModelProperty("包含 schemas")
    private Boolean includeSchemas;

    @ApiModelProperty("包含示例")
    private Boolean includeExamples;

    @ApiModelProperty("包含治理状态")
    private Boolean includeGovernance;
}
