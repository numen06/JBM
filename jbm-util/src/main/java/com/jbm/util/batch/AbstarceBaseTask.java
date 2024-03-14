package com.jbm.util.batch;

import cn.hutool.core.date.DateTime;
import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 批量定时加数量触发任务
 *
 * @author wesley
 */
@Slf4j
public abstract class AbstarceBaseTask<T> extends AbstractScheduledService {

    /**
     * 最大提交时间:毫秒
     */
    protected final Long maxSubmitTime;

    /**
     * 处理时间
     */
    protected final TimeUnit timeUnit;

    /**
     * 最大提交数量
     */
    protected final Integer maxSubmitQuantity;

    private final BlockingQueue<T> blockingQueue;

    /**
     * 构造函数
     *
     * @param maxSubmitTime     最大提交时间
     * @param timeUnit          时间单位
     * @param maxSubmitQuantity 最大提交数量
     * @throws RuntimeException 如果批处理时间和数量同时为0，则抛出运行时异常
     */
    public AbstarceBaseTask(Long maxSubmitTime, TimeUnit timeUnit, Integer maxSubmitQuantity) {
        this.maxSubmitTime = maxSubmitTime;
        this.timeUnit = timeUnit;
        this.maxSubmitQuantity = maxSubmitQuantity;
        if (this.maxSubmitTime + this.maxSubmitQuantity <= 0) {
            throw new RuntimeException("批处理时间和数量不能同时为0");
        }
        blockingQueue = new ArrayBlockingQueue<>(Math.max(maxSubmitQuantity * 2, 10));
        //如果时间为0,则放弃定时执行方式
        if (this.maxSubmitTime > 0) {
            this.startAsync();
        }
    }


    /**
     * 运行一次迭代
     *
     * @throws Exception 可能抛出异常
     */
    @Override
    protected void runOneIteration() throws Exception {
        try {
            asyncAction(new ActionBean<T>(ActionType.TIME, this.blockingQueue, DateTime.now()));
        } catch (Exception e) {
            log.error("批量执行器执行错误,请检查执行方法", e);
        }
    }

    /**
     * 获取调度器
     *
     * @return 调度器对象
     */
    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, maxSubmitTime, timeUnit);
    }

    /**
     * 执行批量操作
     *
     * @param actionBean 批量操作信息
     */
    protected abstract void asyncAction(ActionBean<T> actionBean);

    /**
     * 提交数据
     *
     * @param objs 待提交的数据
     * @return 成功提交的数量
     */
    public void offer(T... objs) {
        if (this.maxSubmitQuantity <= 0) {
            return;
        }
        try {
            for (T data : objs) {
                blockingQueue.put(data);
                if (blockingQueue.size() >= maxSubmitQuantity) {
                    synchronized (blockingQueue) {
                        asyncAction(new ActionBean<T>(ActionType.QUANTITY, blockingQueue, DateTime.now()));
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    /**
     * 添加数据
     *
     * @param objs 待添加的数据
     * @throws RuntimeException 如果添加数量超过最大提交数量，则抛出运行时异常
     */
    @SafeVarargs
    public final void add(T... objs) {
        this.offer(objs);
    }


}
