package com.jbm.util.batch;

import java.util.concurrent.TimeUnit;

/**
 * 批处理工具类
 *
 * @author wesley
 * @createTime 2020年06月24日 14:56:00
 * @description
 */
public class BatchUtils {


    /**
     * 创建一个防抖动的提交任务
     *
     * @param delay
     * @param timeUnit
     * @return DebounceMethod
     */
    public static DebounceMethod createDebounceMethod(long delay, TimeUnit timeUnit) {
        return new DebounceMethod(delay, timeUnit);
    }

    /**
     * 防抖动的提交任务
     *
     * @param delay
     * @param timeUnit
     */
    public static void debounceCall(Runnable task, long delay, TimeUnit timeUnit) {
        BatchUtils.createDebounceMethod(delay, timeUnit).debounceCall(task);
    }

}
