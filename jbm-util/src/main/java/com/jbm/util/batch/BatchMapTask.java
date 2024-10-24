package com.jbm.util.batch;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.BooleanUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 批处理任务类
 *
 * @author wesley
 * @param <T> 任务类型
 */
@Slf4j
public class BatchMapTask<T> extends AbstarceBaseTask<T> {

    private final Consumer<Map<Integer,T>> action;

    private final BlockingQueue<T> blockingQueue;

    /**
     * 构造函数
     *
     * @param action 批量操作的消费函数
     */
    public BatchMapTask(Consumer<Map<Integer,T>> action) {
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
    public BatchMapTask(long maxSubmitTime, TimeUnit timeUnit, int maxSubmitQuantity, Consumer<Map<Integer,T>> action) {
        super(maxSubmitTime, timeUnit, maxSubmitQuantity);
        this.action = action;
        blockingQueue = new ArrayBlockingQueue<>(maxSubmitQuantity < 1 ? 1 : maxSubmitQuantity);
    }

    /**
     * 创建一个批处理任务实例
     *
     * @param action 批量操作的消费函数
     * @return 批处理任务实例
     */
    public static <T> BatchMapTask<T> createBatchTask(final Consumer<Map<Integer,T>> action) {
        return new BatchMapTask<>(action);
    }

    /**
     * 创建一个批处理任务实例
     *
     * @param maxSubmitTime 最大提交时间
     * @param timeUnit      时间单位
     * @param action        批量操作的消费函数
     * @return 批处理任务实例
     */
    public static <T> BatchMapTask<T> createBatchTask(final Long maxSubmitTime, final TimeUnit timeUnit, final Consumer<Map<Integer,T>> action) {
        return new BatchMapTask<>(maxSubmitTime, timeUnit, 200, action);
    }

    /**
     * 创建一个批处理任务实例
     *
     * @param maxSubmitQuantity 最大提交数量
     * @param action            批量操作的消费函数
     * @return 批处理任务实例
     */
    public static <T> BatchMapTask<T> createBatchTask(Integer maxSubmitQuantity, final Consumer<Map<Integer,T>> action) {
        return new BatchMapTask<>(5L, TimeUnit.SECONDS, maxSubmitQuantity, action);
    }

    /**
     * 执行批量操作
     */
    @Override
    protected void asyncAction(ActionBean<T> actionBean) {
        Date startTime = DateTime.now();
        Map<Integer,T> list = new ConcurrentHashMap<>();
        if (actionBean.getCurrQuantity() <= 0) {
            return;
        }
        for (int i = 0; i < actionBean.getCurrQuantity(); i++) {
            T obj = blockingQueue.poll();
            if (obj == null) {
                break;
            }
            list.put(obj.hashCode(),obj);
        }
        Date endTime;
        try {
            action.accept(list);
            endTime = DateTime.now();
            log.debug("批量任务执行成功，执行时间:{}毫秒", DateUtil.between(startTime, endTime, DateUnit.MS));
        } catch (Exception e) {
            endTime = DateTime.now();
            log.error("批量任务执行失败，执行时间:{}毫秒", e, DateUtil.between(startTime, endTime, DateUnit.MS));
        }
    }

    /**
     * 追加元素
     *
     * @param objs 待追加的元素
     * @return 追加成功的元素个数
     */
    @Override
    protected int doOffer(Object... objs) {
        for (int i = 0; i < objs.length; i++) {
            Object obj = objs[i];
            boolean a = this.blockingQueue.offer((T) obj);
            if (BooleanUtil.isFalse(a)) {
                return i;
            }
        }
        return objs.length;
    }

}
