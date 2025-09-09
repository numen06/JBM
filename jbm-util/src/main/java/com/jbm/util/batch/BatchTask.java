package com.jbm.util.batch;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 批处理任务类
 *
 * @param <T> 任务类型
 * @author wesley
 */
@Slf4j
public class BatchTask<T> extends AbstarceBaseTask<T> {

    private final Consumer<List<T>> action;
    private final BlockingQueue<T> blockingQueue;

    public BatchTask(Consumer<List<T>> action) {
        super();
        this.action = action;
        this.blockingQueue = new ArrayBlockingQueue<>(maxSubmitQuantity);
    }

    public BatchTask(Long maxSubmitTime, TimeUnit timeUnit, Integer maxSubmitQuantity, Consumer<List<T>> action) {
        super(maxSubmitTime, timeUnit, maxSubmitQuantity);
        this.action = action;
        this.blockingQueue = new ArrayBlockingQueue<>(Math.max(maxSubmitQuantity, 10));
    }


    @SafeVarargs
    @Override
    protected final void doOffer(AtomicInteger currQuantity, T... objs) {
        for (T obj : objs) {
            if (blockingQueue.offer(obj)) {
                currQuantity.incrementAndGet();
            } else {
                break;
            }
        }
    }

    @Override
    protected void doOfferBlocking(AtomicInteger currQuantity,T obj) throws InterruptedException {
        // 阻塞直到成功
        blockingQueue.put(obj);
        currQuantity.incrementAndGet();
    }

    @Override
    protected int asyncAction(ActionBean<T> actionBean) {
        int size = actionBean.getCurrQuantity();
        final List<T> list = new ArrayList<>();
        if (size <= 0) {
            // 如果为0 则从队列中获取数据
            size = blockingQueue.size();
        }
//        log.info("需要从队列中取出{}个，目前队列中有{}个", size, blockingQueue.size() );
        // 从队列中获取数据
        while (list.size() < size) {
            T obj = blockingQueue.poll();
            list.add(obj);
        }
//        log .info("从队列中取出{}个，目前队列中有{}个", list.size(), blockingQueue.size() );
        try {
            action.accept(list);
            log.debug("批量任务执行成功，数量：{}，触发方式：{}，耗时：{}ms",
                    list.size(), actionBean.getActionType(),
                    DateUtil.between(actionBean.getSubmitTime(), DateTime.now(), DateUnit.MS));
        } catch (Exception e) {
            log.error("批量任务执行失败，数量：{}", list.size(), e);
        }
        return list.size();
    }
}