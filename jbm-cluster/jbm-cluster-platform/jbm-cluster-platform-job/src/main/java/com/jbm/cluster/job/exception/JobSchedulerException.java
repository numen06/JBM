package com.jbm.cluster.job.exception;

/**
 * 任务调度相关异常（替代原 Quartz {@code org.quartz.SchedulerException} 在业务接口中的使用）。
 */
public class JobSchedulerException extends Exception {

    public JobSchedulerException(String message) {
        super(message);
    }

    public JobSchedulerException(String message, Throwable cause) {
        super(message, cause);
    }
}
