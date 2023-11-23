package com.jbm.cluster.api.model.gateway;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
//@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class GatewayLogInfo implements Serializable {

    @Id
    @ApiModelProperty(value = "访问ID")
    private String accessId;

    @ApiModelProperty(value = "日志等级")
    private Integer loglevel;

    @ApiModelProperty(value = "路径")
    private String path;

    @ApiModelProperty(value = "接口路径")
    private String apiPath;

    @ApiModelProperty(value = "请求人ID")
    private Long requestUserId;

    @ApiModelProperty(value = "请求人")
    private String requestRealName;

    @ApiModelProperty(value = "接口名称")
    private String apiName;

    @ApiModelProperty(value = "操作类型")
    private String operationType;

//    @ApiModelProperty(value = "操作反馈")
//    private String operationFeedback;

    @ApiModelProperty(value = "应用ID")
    private Long appId;

    @ApiModelProperty(value = "应用Key")
    private String appKey;

    @ApiModelProperty(value = "应用名称")
    private String appName;

    @ApiModelProperty(value = "请求类型")
    private String method;

    @ApiModelProperty(value = "请求IP")
    private String ip;

    @ApiModelProperty(value = "响应状态")
    private Integer httpStatus;

    @ApiModelProperty(value = "请求时间")
    private Date requestTime;

    @ApiModelProperty(value = "响应时间")
    private Date responseTime;

    @ApiModelProperty(value = "响应时间")
    private String responseBody;

    @ApiModelProperty(value = "耗时")
    private Long useTime;

    @ApiModelProperty(value = "请求数据")
    private String params;

    @ApiModelProperty(value = "请求头")
    private String headers;

    @ApiModelProperty(value = "用户权限")
    private String userAgent;

    @ApiModelProperty(value = "区域")
    private String region;

    @ApiModelProperty(value = "认证用户信息")
    private String authentication;

    @ApiModelProperty(value = "服务名")
    private String serviceId;

    @ApiModelProperty(value = "错误信息")
    private String error;
}
