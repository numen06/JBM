package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.util.Date;

/**
 * 开放网关-访问日志
 *
 * @author wesley.zhang
 */
@Data
@Entity
@NoArgsConstructor
@TableName("gateway_access_logs")
public class GatewayAccessLogs extends MasterDataEntity {


    /**
     * 访问路径
     */
    private String path;

    /**
     * 请求类型
     */
    private String method;

    /**
     * 请求IP
     */
    private String ip;

    /**
     * 响应状态
     */
    private String httpStatus;

    /**
     * 请求时间
     */
    private Date requestTime;

    /**
     * 响应时间
     */
    private Date responseTime;

    /**
     * 耗时
     */
    private Long useTime;

    /**
     * 请求数据
     */
    private String params;

    /**
     * 请求头
     */
    private String headers;

    private String userAgent;

    /**
     * 区域
     */
    private String region;

    /**
     * 认证用户信息
     */
    private String authentication;

    /**
     * 服务名
     */
    private String serviceId;

    /**
     * 错误信息
     */
    private String error;

    private static final long serialVersionUID = 1L;
}
