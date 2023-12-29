package com.jbm.cluster.core.constant;

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
//    public static final String QUEUE_SCAN_DIC_RESOURCE = "cluster.scan.dic";

    /**
     * 扫描事件方法
     */
    public static final String QUEUE_SCAN_EVENT = "cluster.scan.event";
    /**
     * 扫描事件方法
     */
//    public static final String QUEUE_SCAN_SCHEDULED = "cluster.scan.scheduled";
    /**
     * 访问日志
     */
    public static final String QUEUE_ACCESS_LOGS = "cloud.access.logs";
    /**
     * 推送消息
     */
    public static final String QUEUE_PUSH_MESSAGE = "cloud.push.message.test";
    /**
     * 集群事件
     */
    public static final String QUEUE_CLUSTER_EVENT = "cluster.event";
    /**
     * 集群事件路由器
     */
    public static final String QUEUE_CLUSTER_EVENT_EXCHANGE = QUEUE_CLUSTER_EVENT + ".exchange";


    public static final String DIC_RESOURCE_STREAM = "dicResource-in-0";

    public static final String BUSINESS_EVENT_RESOURCE_STREAM = "businessEventResource-in-0";

    public static final String BUSINESS_EVENT_STREAM = "businessEvent-in-0";
    /**
     * 字典通道
     */
    public static final String SCHEDULED_JOB_STREAM = "scheduledJob-in-0";


    /**
     * 通知通道
     */
    public static final String NOTIFICATION_STREAM = "notification-in-0";
    /**
     * 访问日志
     */
    public static final String ACCESS_LOGS_STREAM = "accessLogs-in-0";

    /**
     * 访问日志
     */
    public static final String ACCOUNT_LOGS_STREAM = "accountLogs-in-0";

    /**
     * 访问日志
     */
    public static final String API_RESOURCE_STREAM = "apiResource-in-0";

    /**
     * 站内信
     */
    public static final String PUSH_MESSAGE_STREAM = "pushMsg-in-0";


}
