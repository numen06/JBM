package com.jbm.util.batch;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
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
        this.blockingQueue = new ArrayBlockingQueue<>(Math.max(maxSubmitQuantity, 200));
    }


    @Override
    protected int doOffer(T... objs) {
        int count = 0;
        for (T obj : objs) {
            if (blockingQueue.offer(obj)) {
                count++;
            } else {
                break; // 队列满，停止
            }
        }
        return count;
    }

    @Override
    protected void doOfferBlocking(T obj) throws InterruptedException {
        blockingQueue.put(obj); // 阻塞直到成功
    }

    @Override
    protected void asyncAction(ActionBean<T> actionBean) {
        int size = actionBean.getCurrQuantity();
        if (size <= 0) return;

        List<T> list = new ArrayList<>(size);
        blockingQueue.drainTo(list, size); // 安全取出最多 size 个

        // 补齐：如果 drainTo 没取够（并发消费），再 poll 一下
        while (list.size() < size && !blockingQueue.isEmpty()) {
            T item = blockingQueue.poll();
            if (item != null) list.add(item);
        }
        try {
            action.accept(list);
            log.debug("批量任务执行成功，数量：{}，触发方式：{}，耗时：{}ms",
                    list.size(), actionBean.getActionType(),
                    DateUtil.between(actionBean.getSubmitTime(), DateTime.now(), DateUnit.MS));
        } catch (Exception e) {
            log.error("批量任务执行失败，数量：{}", list.size(), e);
        }
    }
}