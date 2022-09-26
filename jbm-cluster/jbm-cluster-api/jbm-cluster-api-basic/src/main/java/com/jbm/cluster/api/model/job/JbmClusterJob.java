package com.jbm.cluster.api.model.job;

import lombok.Data;

import java.io.Serializable;

/**
 * @author wesley
 */
@Data
public class JbmClusterJob implements Serializable {

    /**
     * 事件类型
     */
    private String eventType;
    /**
     * 任务名称
     */
    private String jobName;

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
