package com.jbm.cluster.api.model.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 业务日志请求对象
 * 用于Feign客户端调用
 * 
 * @author wesley
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessLogRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务类型（如：订单、支付、用户等）
     */
    private String businessType;

    /**
     * 业务ID（如：订单号、支付流水号等）
     */
    private String businessId;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 过期天数（7、30、90、180、365）
     * 默认30天
     */
    private Integer expireDays = 30;

    /**
     * 日志来源（如：订单服务、支付服务等）
     */
    private String source;

    /**
     * 操作人（可选）
     */
    private String operator;

    /**
     * 操作人ID（可选）
     */
    private String operatorId;

    /**
     * 租户ID（多租户场景使用）
     */
    private String tenantId;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 扩展字段（JSON格式）
     */
    private String extData;
}

