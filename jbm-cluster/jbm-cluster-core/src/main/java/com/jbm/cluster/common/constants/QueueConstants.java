package com.jbm.cluster.common.constants;

/**
 * 消息队列静态变量
 *
 * @author: wesley.zhang
 * @date: 2019/2/21 17:46
 * @description:
 */
public class QueueConstants {
    /**
     * 扫描API
     */
    public static final String QUEUE_SCAN_API_RESOURCE = "cloud.scan.api.resource";
    /**
     * 扫描字典
     */
    public static final String QUEUE_SCAN_DIC_RESOURCE = "cluster.scan.dic";
    /**
     * 访问日志
     */
    public static final String QUEUE_ACCESS_LOGS = "cloud.access.logs";
    /**
     * 推送消息
     */
    public static final String QUEUE_PUSH_MESSAGE = "cloud.push.message";
    /**
     * 集群时间
     */
    public static final String QUEUE_CLUSTER_EVENT = "cluster.event";
    public static final String QUEUE_CLUSTER_EVENT_EXCHANGE = QUEUE_CLUSTER_EVENT + ".exchange";
}
