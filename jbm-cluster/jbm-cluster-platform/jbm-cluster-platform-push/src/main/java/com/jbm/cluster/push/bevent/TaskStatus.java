package com.jbm.cluster.push.bevent;

/**
 * 任务状态
 * @author wesley
 */
public enum TaskStatus {
    PENDING,
    SUCCESS,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELED,
    TIMEOUT,
    RETRYING,
    RETRY_FAILED,
    RETRY_CANCELED,
    RETRY_TIMEOUT,
    RETRY_COMPLETED,
    RETRY_FAILED_COMPLETED,
    RETRY_CANCELED_COMPLETED,
    RETRY_TIMEOUT_COMPLETED,

}
