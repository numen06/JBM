package com.jbm.util.batch;

import cn.hutool.core.collection.CollUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * 滚动任务执行器
 *
 * @author wesley
 */
public class RollingTask<T> extends AbstarceBaseTask<T> {

    private final Function<ActionBean<T>, T> action;

    /**
     * 使用 AtomicReference 类来创建一个私有变量 atomicReference，该变量的初始值为 null
     */
    private final AtomicReference<T> atomicReference = new AtomicReference<>(null);

    /**
     * 构造一个滚动任务执行器
     *
     * @param action 任务执行函数
     */
    public RollingTask(Function<ActionBean<T>, T> action) {
        this(5L, TimeUnit.SECONDS, 200, action);
    }

    /**
     * 构造一个滚动任务执行器
     *
     * @param maxSubmitTime     最大提交时间
     * @param timeUnit          时间单位
     * @param maxSubmitQuantity 最大提交数量
     * @param action            任务执行函数
     */
    public RollingTask(Long maxSubmitTime, TimeUnit timeUnit, Integer maxSubmitQuantity, Function<ActionBean<T>, T> action) {
        super(maxSubmitTime, timeUnit, maxSubmitQuantity);
        this.action = action;
    }

    /**
     * 创建一个滚动任务执行器
     *
     * @param action 任务执行函数
     * @return 滚动任务执行器实例
     */
    public static <T> RollingTask<T> createRollingTask(final Function<ActionBean<T>, T> action) {
        return new RollingTask<T>(action);
    }

    /**
     * 创建一个滚动任务执行器
     *
     * @param maxSubmitTime 最大提交时间
     * @param timeUnit      时间单位
     * @param action        任务执行函数
     * @return 滚动任务执行器实例
     */
    public static <T> RollingTask<T> createRollingTask(final Long maxSubmitTime, final TimeUnit timeUnit, final Function<ActionBean<T>, T> action) {
        return new RollingTask<T>(maxSubmitTime, timeUnit, 0, action);
    }

    /**
     * 创建一个滚动任务执行器
     *
     * @param maxSubmitQuantity 最大提交数量
     * @param action            任务执行函数
     * @return 滚动任务执行器实例
     */
    public static <T> RollingTask<T> createRollingTask(Integer maxSubmitQuantity, final Function<ActionBean<T>, T> action) {
        return new RollingTask<T>(0L, TimeUnit.SECONDS, maxSubmitQuantity, action);
    }

    /**
     * 异步执行任务
     *
     * @param actionBean 任务
     */
    @Override
    protected int asyncAction(ActionBean actionBean) {
        if(ActionType.TIME == actionBean.getActionType()){
            actionBean = new ActionBean(ActionType.TIME, this.getCurrQuantity(), actionBean.getSubmitTime());
        }
        actionBean.setObj(atomicReference.get());
        this.atomicReference.set((T) action.apply(actionBean));
        return actionBean.getCurrQuantity();
    }

    /**
     * 提交任务
     *
     * @param objs 参数
     * @return 提交结果
     */
    @Override
    protected void doOffer(AtomicInteger currQuantity, T... objs) {
        CollUtil.newArrayList(objs).forEach(obj -> {
            atomicReference.set((T) obj);
        });
        int count = objs.length == 0 ? 1 : objs.length;
        currQuantity.addAndGet(count);
    }

    /**
     * @param obj
     * @throws InterruptedException
     */
    @Override
    protected void doOfferBlocking(AtomicInteger currQuantity,T obj) throws InterruptedException {
        atomicReference.getAndSet((T) obj);
        currQuantity.incrementAndGet();
    }
}
