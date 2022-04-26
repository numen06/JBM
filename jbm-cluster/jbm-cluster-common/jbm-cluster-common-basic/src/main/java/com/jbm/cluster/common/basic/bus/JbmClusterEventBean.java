package com.jbm.cluster.common.basic.bus;

import lombok.Data;

@Data
public class JbmClusterEventBean {

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
