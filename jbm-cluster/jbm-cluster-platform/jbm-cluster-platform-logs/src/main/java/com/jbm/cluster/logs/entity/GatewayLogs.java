package com.jbm.cluster.logs.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2021-05-06 16:52
 **/
@Data
@Document("gateway_logs")
public class GatewayLogs implements Serializable {
    /**
     * 访问ID
     */
    @Id
    @ApiModelProperty(value = "访问ID")
    private String accessId;

    /**
     * 访问路径
     */
    @ApiModelProperty(value = "路径")
    private String path;

    /**
     * 请求类型
     */
    @ApiModelProperty(value = "请求类型")
    private String method;

    /**
     * 请求IP
     */
    @ApiModelProperty(value = "请求IP")
    private String ip;

    /**
     * 响应状态
     */
    @ApiModelProperty(value = "响应状态")
    private String httpStatus;

    /**
     * 请求时间
     */
    @ApiModelProperty(value = "请求时间")
    private Date requestTime;

    /**
     * 响应时间
     */
    @ApiModelProperty(value = "响应时间")
    private Date responseTime;

    /**
     * 耗时
     */
    @ApiModelProperty(value = "耗时")
    private Long useTime;

    /**
     * 请求数据
     */
    @ApiModelProperty(value = "请求数据")
    private String params;

    /**
     * 请求头
     */
    @ApiModelProperty(value = "请求头")
    private String headers;

    /**
     * 用户权限
     */
    @ApiModelProperty(value = "用户权限")
    private String userAgent;

    /**
     * 区域
     */
    @ApiModelProperty(value = "区域")
    private String region;

    /**
     * 认证用户信息
     */
    @ApiModelProperty(value = "认证用户信息")
    private String authentication;

    /**
     * 服务名
     */
    @ApiModelProperty(value = "服务名")
    private String serviceId;

    /**
     * 错误信息
     */
    @ApiModelProperty(value = "错误信息")
    private String error;


}
