package com.jbm.util.batch;

import cn.hutool.core.date.DateUtil;
import com.google.common.util.concurrent.AbstractScheduledService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class BatchTask<T> extends AbstractScheduledService {


    /**
     * 最大提交时间:毫秒
     */
    private final Long maxSubmitTime;

    /**
     * 处理时间
     */
    private final TimeUnit timeUnit;

    /**
     * 最大提交数量
     */
    private final Integer maxSubmitQuantity;

    private final Consumer<List<T>> action;


    private BlockingQueue<T> blockingQueue;


    public BatchTask(Consumer<List<T>> action) {
        this(5L, TimeUnit.SECONDS, 200, action);
    }


    public BatchTask(Long maxSubmitTime, TimeUnit timeUnit, Integer maxSubmitQuantity, Consumer<List<T>> action) {
        this.maxSubmitTime = maxSubmitTime;
        this.timeUnit = timeUnit;
        this.maxSubmitQuantity = maxSubmitQuantity;
        this.action = action;
        blockingQueue = new ArrayBlockingQueue<>(maxSubmitQuantity);
        this.startAsync();
    }

    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    protected void runOneIteration() throws Exception {
        asyncAction();
    }


    /**
     * 执行批量操作
     */
    private void asyncAction() {
        List list = new ArrayList();
        synchronized (blockingQueue) {
            final Integer len = blockingQueue.size();
            if (len <= 0) {
                return;
            }
            for (int i = 0; i < len; i++) {
                list.add(blockingQueue.poll());
            }
        }
        action.accept(list);
    }


    /**
     * 追加元素
     *
     * @param obj
     */
    public void offer(T obj) {
        blockingQueue.offer(obj);
        if (blockingQueue.size() >= maxSubmitQuantity) {
            asyncAction();
        }
    }


    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, maxSubmitTime, timeUnit);
    }

    public static <T> BatchTask<T> createBatchTask(final Consumer<List<T>> action) {
        return new BatchTask<>(action);
    }

    public static <T> BatchTask<T> createBatchTask(final Long maxSubmitTime, final TimeUnit timeUnit, final Integer maxSubmitQuantity, final Consumer<List<T>> action) {
        return new BatchTask<>(maxSubmitTime, timeUnit, maxSubmitQuantity, action);
    }

}
