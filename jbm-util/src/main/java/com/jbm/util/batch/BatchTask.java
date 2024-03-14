package com.jbm.util.batch;

import cn.hutool.core.date.StopWatch;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 批处理任务类
 *
 * @param <T> 任务类型
 */
@Slf4j
public class BatchTask<T> extends AbstarceBaseTask<T> {

    private final Consumer<List<T>> action;

    /**
     * 构造函数
     *
     * @param action 批量操作的消费函数
     */
    public BatchTask(Consumer<List<T>> action) {
        this(5L, TimeUnit.SECONDS, 200, action);
    }

    /**
     * 构造函数
     *
     * @param maxSubmitTime     最大提交时间
     * @param timeUnit          时间单位
     * @param maxSubmitQuantity 最大提交数量
     * @param action            批量操作的消费函数
     */
    public BatchTask(long maxSubmitTime, TimeUnit timeUnit, int maxSubmitQuantity, Consumer<List<T>> action) {
        super(maxSubmitTime, timeUnit, maxSubmitQuantity);
        this.action = action;

    }

    /**
     * 创建一个批处理任务实例
     *
     * @param action 批量操作的消费函数
     * @return 批处理任务实例
     */
    public static <T> BatchTask<T> createBatchTask(final Consumer<List<T>> action) {
        return new BatchTask<>(action);
    }

    /**
     * 创建一个批处理任务实例
     *
     * @param maxSubmitTime 最大提交时间
     * @param timeUnit      时间单位
     * @param action        批量操作的消费函数
     * @return 批处理任务实例
     */
    public static <T> BatchTask<T> createBatchTask(final Long maxSubmitTime, final TimeUnit timeUnit, final Consumer<List<T>> action) {
        return new BatchTask<>(maxSubmitTime, timeUnit, 200, action);
    }

    /**
     * 创建一个批处理任务实例
     *
     * @param maxSubmitQuantity 最大提交数量
     * @param action            批量操作的消费函数
     * @return 批处理任务实例
     */
    public static <T> BatchTask<T> createBatchTask(Integer maxSubmitQuantity, final Consumer<List<T>> action) {
        return new BatchTask<>(5L, TimeUnit.SECONDS, maxSubmitQuantity, action);
    }


    /**
     * 执行批量操作
     */
    @Override
    protected void asyncAction(ActionBean<T> actionBean) {
        if (actionBean.getBlockingQueue().isEmpty()) {
            return;
        }
        StopWatch stopWatch = StopWatch.create("批量执行任务");
        try {
            stopWatch.start();
            // 创建一个List来接收队列中的元素
            List<T> list = new ArrayList<>();
            // 使用while循环将队列中的所有元素转移到List中
            while (!actionBean.getBlockingQueue().isEmpty()) {
                list.add(actionBean.getBlockingQueue().poll());
            }
            action.accept(list);
            stopWatch.stop();
            if (stopWatch.getTotal(TimeUnit.SECONDS) > 1) {
                log.warn("批量任务执行成功，执行时间:{}秒", stopWatch.getTotal(TimeUnit.SECONDS));
            }
        } catch (Exception e) {
            log.error("批量任务执行失败，执行时间:{}秒", stopWatch.getTotal(TimeUnit.SECONDS));
        }
    }

}
