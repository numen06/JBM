package com.jbm.util.batch;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.BooleanUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


@Slf4j
public class BatchTask<T> extends AbstarceBaseTask {

    private final Consumer<List<T>> action;

    private BlockingQueue<T> blockingQueue;




    public BatchTask(Consumer<List<T>> action) {
        this(5L, TimeUnit.SECONDS, 200, action);
    }

    public BatchTask(long maxSubmitTime, TimeUnit timeUnit, int maxSubmitQuantity, Consumer<List<T>> action) {
        super(maxSubmitTime, timeUnit, maxSubmitQuantity);
        this.action = action;
        blockingQueue = new ArrayBlockingQueue<>(maxSubmitQuantity< 1 ? 1: maxSubmitQuantity);
    }



    /**
     * 执行批量操作
     */
    @Override
    protected void asyncAction(ActionBean actionBean) {
        Date startTime = DateTime.now();
        List list = new ArrayList();
        if (actionBean.getCurrQuantity() <= 0) {
            return;
        }
        for (int i = 0; i < actionBean.getCurrQuantity(); i++) {
            list.add(blockingQueue.poll());
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
     * @param objs
     */
    @Override
    protected int doOffer(Object... objs) {
        for (int i = 0; i < objs.length; i++) {
            Object obj = objs[i];
            boolean a = this.blockingQueue.offer((T) obj);
            if (BooleanUtil.isFalse(a)) {
                return i ;
            }
        }
        return objs.length;
    }





    public static <T> BatchTask<T> createBatchTask(final Consumer<List<T>> action) {
        return new BatchTask<>(action);
    }

    public static <T> BatchTask<T> createBatchTask(final Long maxSubmitTime, final TimeUnit timeUnit, final Consumer<List<T>> action) {
        return new BatchTask<>(maxSubmitTime, timeUnit, 200, action);
    }

    public static <T> BatchTask<T> createBatchTask(Integer maxSubmitQuantity, final Consumer<List<T>> action) {
        return new BatchTask<>(5L, TimeUnit.SECONDS, maxSubmitQuantity, action);
    }

}
