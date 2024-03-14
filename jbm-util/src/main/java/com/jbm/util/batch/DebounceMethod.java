package com.jbm.util.batch;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 防抖执行方法
 *
 * @author wesley
 * @Version 1.0
 * @Description
 */
public class DebounceMethod {
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    private final long delay;
    private final TimeUnit timeUnit;

    public DebounceMethod(long delay, TimeUnit timeUnit) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.delay = delay;
        this.timeUnit = timeUnit;
    }

    public void debounceCall(Runnable task) {
        debounceCall(task, this.delay, this.timeUnit);
    }

    // 延迟执行的方法
    private synchronized void debounceCall(Runnable task, long delay, TimeUnit timeUnit) {
        // 如果之前已经有任务被安排，则取消它
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }
        // 安排新的任务在延迟时间过后执行
        scheduledTask = scheduler.schedule(task, delay, timeUnit);
    }

    // 当不再需要这个实例时，记得关闭线程池
    public void shutdown() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
