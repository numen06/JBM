package com.jbm.util.batch;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;


/**
 * 滚动任务执行器
 */
public class RollingTask<T> extends AbstarceBaseTask {

    private final Function<ActionBean<T>,T> action;


    private AtomicReference<T> atomicReference = new AtomicReference<>(null);

    public RollingTask(Function<ActionBean<T>,T> action) {
        this(5L, TimeUnit.SECONDS, 200, action);
    }

    public RollingTask(Long maxSubmitTime, TimeUnit timeUnit, Integer maxSubmitQuantity, Function<ActionBean<T>,T> action) {
        super(maxSubmitTime, timeUnit, maxSubmitQuantity);
        this.action = action;
    }

    @Override
    protected  void asyncAction(ActionBean actionBean) {
        actionBean.setObj(atomicReference.get());
        this.atomicReference.set((T) action.apply(actionBean));
    }

    @Override
    protected int doOffer(Object... obj) {
        return 1;
    }


    public static <T> RollingTask createRollingTask(final Function<ActionBean<T>,T> action) {
        return new RollingTask(action);
    }

    public static <T> RollingTask createRollingTask(final Long maxSubmitTime, final TimeUnit timeUnit, final Function<ActionBean<T>,T> action) {
        return new RollingTask(maxSubmitTime, timeUnit, 0, action);
    }

    public static <T> RollingTask createRollingTask(Integer maxSubmitQuantity, final Function<ActionBean<T>,T> action) {
        return new RollingTask(0L, TimeUnit.SECONDS, maxSubmitQuantity, action);
    }

}
