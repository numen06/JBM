package com.jbm.util.batch;

import cn.hutool.core.date.DateTime;
import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 批量定时加数量触发任务
 *
 * @author wesley
 */
@Slf4j
public abstract class AbstarceBaseTask<T> extends AbstractScheduledService {

    // 最大提交时间（毫秒）
    protected final Long maxSubmitTime;
    // 时间单位
    protected final TimeUnit timeUnit;
    // 最大提交数量
    protected final Integer maxSubmitQuantity;

    // 当前累积数量，volatile + 原子操作保证可见性和原子性
    private final AtomicInteger currQuantity = new AtomicInteger(0);

    // 最后一次触发时间
    private final AtomicLong lastActionTime = new AtomicLong(System.currentTimeMillis());

    public AbstarceBaseTask() {
        this(5L, TimeUnit.SECONDS, 200);
    }

    public AbstarceBaseTask(Integer maxSubmitQuantity) {
        this(null, null, maxSubmitQuantity);
    }

    public AbstarceBaseTask(Long maxSubmitTime, TimeUnit timeUnit) {
        this(maxSubmitTime, timeUnit, 200);
    }

    public AbstarceBaseTask(Long maxSubmitTime, TimeUnit timeUnit, Integer maxSubmitQuantity) {
        if (maxSubmitTime == null || maxSubmitQuantity == null) {
            throw new IllegalArgumentException("maxSubmitTime and maxSubmitQuantity cannot be null");
        }
        this.maxSubmitTime = maxSubmitTime;
        this.timeUnit = timeUnit;
        // 至少为1
        this.maxSubmitQuantity = Math.max(maxSubmitQuantity, 0);

        if (this.maxSubmitTime <= 0 && this.maxSubmitQuantity <= 0) {
            throw new IllegalArgumentException("批处理时间和数量不能同时为0或负数");
        }
        // 只有时间 > 0 才启动定时任务
        if (this.maxSubmitTime > 0) {
            this.startAsync();
        }
    }


    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, maxSubmitTime, timeUnit);
    }

    @Override
    protected void runOneIteration() {
        // 时间触发
        submitIfNotEmpty(ActionType.TIME);
    }

    /**
     * 提交数据：非阻塞，尝试添加到队列
     */
    @SafeVarargs
    public final int offer(T... objs) {
//        if (objs == null || objs.length == 0) {
//            return 0;
//        }

        // 1. 先尝试入队（由子类实现，如 BlockingQueue.put 或 offer）
        int addedCount = doOffer(this.currQuantity, objs);
//        if (addedCount <= 0) {
//            return 0;
//        }

        // 2. 原子增加计数
        int newTotal = currQuantity.addAndGet(addedCount);

        // 3. 检查是否达到数量阈值
        if (maxSubmitQuantity > 0 & newTotal >= maxSubmitQuantity) {
            submitIfNotEmpty(ActionType.QUANTITY);
        }

        return addedCount;
    }

    /**
     * 阻塞提交：等待直到成功入队
     */
    @SafeVarargs
    public final void offerBlocking(T... objs) {
        if (objs == null) {
            return;
        }
        for (T obj : objs) {
            try {
                // 子类实现阻塞入队
                doOfferBlocking(obj);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            int newTotal = currQuantity.incrementAndGet();
            if (maxSubmitQuantity > 0 & newTotal >= maxSubmitQuantity) {
                submitIfNotEmpty(ActionType.QUANTITY);
            }
        }
    }


    /**
     * 尝试提交，如果当前有数据且未在提交中
     */
    protected synchronized final void submitIfNotEmpty(ActionType triggerType) {
        final int count = currQuantity.get();
        int doCount = 0;
        try {
            if (count <= 0) {
                return;
            }
            //如果两次触发时间低于最小时间则不触发
            if (ActionType.TIME.equals(triggerType)) {
                long tc = System.currentTimeMillis() - lastActionTime.get();
                if (TimeUnit.MILLISECONDS.toMicros(tc) < timeUnit.toMicros(maxSubmitTime)) {
                    return;
                }
            }
            lastActionTime.set(System.currentTimeMillis());
            DateTime actionTime = DateTime.of(lastActionTime.get());
            ActionBean<T> actionBean = new ActionBean<>(triggerType, count, actionTime);
            doCount = asyncAction(actionBean);
        } catch (Exception e) {
            log.error("批量执行器执行失败", e);
        } finally {
            currQuantity.addAndGet(-doCount);
        }
    }

    /**
     * 强制提交当前所有数据（无论数量或时间）
     */
    public void flush() {
        submitIfNotEmpty(ActionType.FLUSH);
    }

    /**
     * 子类实现：非阻塞添加数据
     */
    protected abstract int doOffer(AtomicInteger currQuantity, T... obj);

    /**
     * 子类实现：阻塞添加单个元素（用于 offerBlocking）
     */
    protected abstract void doOfferBlocking(T obj) throws InterruptedException;

    /**
     * 子类实现：异步处理逻辑
     */
    protected abstract int asyncAction(ActionBean<T> actionBean);


}
