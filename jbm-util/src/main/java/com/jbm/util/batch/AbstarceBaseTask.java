package com.jbm.util.batch;

import cn.hutool.core.date.DateTime;
import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    protected final AtomicInteger currQuantity = new AtomicInteger(0);


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
            synchronized (this.currQuantity) {
                asyncAction(new ActionBean(ActionType.TIME, this.currQuantity.get(), DateTime.now()));
                this.currQuantity.set(0);
            }
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
    public int offer(T... objs) {
        int len = 0;
        try {
            len = this.doOffer(objs);
            if (len <= 0) {
                return len;
            }
            currQuantity.getAndAdd(len);
        } catch (Exception e) {
            return len;
        }
        //如果提交数量是<1那么不执行数量触发
        if (this.currQuantity.get() >= this.maxSubmitQuantity) {
            try {
                synchronized (this.currQuantity) {
                    if (this.maxSubmitQuantity > 0) {
                        asyncAction(new ActionBean(ActionType.QUANTITY, this.currQuantity.get(), DateTime.now()));
                        this.currQuantity.set(0);
                    }
//                    this.currQuantity.set(0);
                }
            } catch (Exception e) {
                log.error("批量执行器执行错误,请检查执行方法", e);
            }
        }
        return len;
    }


    /**
     * 等待提交数据
     *
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @param objs     待提交的数据
     * @return 成功提交的数量
     * @throws InterruptedException 如果线程中断，则抛出中断异常
     */
    public int offerOfWait(long timeout, TimeUnit timeUnit, T... objs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (true) {
            int w = this.offer(objs);
            // 尝试写入
            if (w > 0) {
                return w;
            }
            // 超时处理
            if (timeUnit.toMillis(System.currentTimeMillis() - startTime) >= timeout) {
                return 0;
            }
            // 等待100毫秒
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

    /**
     * 添加数据
     *
     * @param objs 待添加的数据
     * @throws RuntimeException 如果添加数量超过最大提交数量，则抛出运行时异常
     */
    public void add(T... objs) {
        if (this.offer(objs) <= 0) {
            throw new RuntimeException("添加数量超过最大提交数量");
        }
    }


    /**
     * 执行添加数据操作
     *
     * @param obj 待添加的数据
     * @return 成功添加的数量
     */
    protected abstract int doOffer(T... obj);


}
