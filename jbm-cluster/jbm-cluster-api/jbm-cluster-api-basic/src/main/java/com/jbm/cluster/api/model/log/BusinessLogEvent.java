package com.jbm.cluster.api.model.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 业务日志事件
 * 用于在消息队列中传递业务日志操作请求
 * 
 * @author wesley
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessLogEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    private BusinessLogEventType eventType;

    /**
     * 日志ID（用于追加、删除、查询等操作）
     */
    private String logId;

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
     */
    private Integer expireDays;

    /**
     * 日志来源（如：订单服务、支付服务等）
     */
    private String source;

    /**
     * 事件时间戳
     */
    private Long timestamp;

    /**
     * 过期分钟数（用于生成临时URL）
     */
    private Integer expireMinutes;

    /**
     * 扩展字段（JSON格式）
     */
    private String extData;

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
}

