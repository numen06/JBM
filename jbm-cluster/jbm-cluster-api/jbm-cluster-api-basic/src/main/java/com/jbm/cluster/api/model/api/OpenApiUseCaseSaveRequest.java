package com.jbm.cluster.api.model.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

@Data
@ApiModel("OpenAPI 用例保存请求")
public class OpenApiUseCaseSaveRequest {

    @ApiModelProperty("用例名称")
    private String name;

    @ApiModelProperty("用例说明")
    private String description;

    @ApiModelProperty("路径参数")
    private Map<String, String> pathParams;

    @ApiModelProperty("查询参数")
    private Map<String, String> queryParams;

    @ApiModelProperty("请求头")
    private Map<String, String> headers;

    @ApiModelProperty("请求体")
    private String body;

    @ApiModelProperty("测试目标 URL")
    private String requestUrl;

    @ApiModelProperty("测试是否成功")
    private Boolean success;

    @ApiModelProperty("响应状态码")
    private Integer responseStatus;

    @ApiModelProperty("响应头")
    private Map<String, String> responseHeaders;

    @ApiModelProperty("响应体")
    private String responseBody;

    @ApiModelProperty("错误类型")
    private String errorType;

    @ApiModelProperty("错误信息")
    private String errorMessage;

    @ApiModelProperty("耗时毫秒")
    private Long durationMs;
}
