package com.jbm.cluster.api.model.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

@Data
@ApiModel("OpenAPI 测试响应")
public class OpenApiTestResult {

    @ApiModelProperty("是否成功")
    private Boolean success;

    @ApiModelProperty("HTTP 状态码")
    private Integer status;

    @ApiModelProperty("耗时毫秒")
    private Long durationMs;

    @ApiModelProperty("响应头")
    private Map<String, String> headers;

    @ApiModelProperty("响应体预览")
    private String bodyPreview;

    @ApiModelProperty("是否截断")
    private Boolean truncated;

    @ApiModelProperty("目标描述")
    private String target;

    @ApiModelProperty("实际请求 URL")
    private String requestUrl;

    @ApiModelProperty("错误类型")
    private String errorType;

    @ApiModelProperty("错误信息")
    private String errorMessage;

    @ApiModelProperty("是否携带调用者 Authorization")
    private Boolean authorizationApplied;
}
