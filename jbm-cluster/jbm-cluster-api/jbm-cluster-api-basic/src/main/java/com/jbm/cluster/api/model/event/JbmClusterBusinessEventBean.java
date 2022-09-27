package com.jbm.cluster.api.model.event;

import lombok.Data;

import java.io.Serializable;

/**
 * @author wesley
 */
@Data
public class JbmClusterBusinessEventBean implements Serializable {
    /**
     * 事件编码
     */
    private String eventCode;

    /**
     * 事件名称
     */
    private String eventName;
    /**
     * 事件分组
     */
    private String eventGroup;

    /**
     * 时间内容体
     */
    private String eventBody;
    /**
     * 触发地址
     */
    private String url;

    /**
     * 集群服务的ID
     */
    private String serviceName;

    /**
     * 定时任务
     */
    private String cron;

    /**
     * 是否启用
     */
    private Boolean enable;
    /**
     * 方法类型
     */
    private String methodType;
    /**
     * 方法内容类型
     */
    private String contentType;
    /**
     * 描述
     */
    private String description;

}
