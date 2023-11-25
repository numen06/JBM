package com.jbm.util.batch;

import cn.hutool.core.date.DateTime;
import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 批量定时加数量触发任务
 * @author wesley
 */
@Slf4j
public abstract class AbstarceBaseTask extends AbstractScheduledService {

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


    public AbstarceBaseTask(Long maxSubmitTime, TimeUnit timeUnit, Integer maxSubmitQuantity) {
        this.maxSubmitTime = maxSubmitTime;
        this.timeUnit = timeUnit;
        this.maxSubmitQuantity = maxSubmitQuantity;
        if(this.maxSubmitTime+ this.maxSubmitQuantity <=0) {
            throw new RuntimeException("批处理时间和数量不能同时为0");
        }
        //如果时间为0,则放弃定时执行方式
        if (this.maxSubmitTime  > 0) {
            this.startAsync();
        }
    }


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


    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, maxSubmitTime, timeUnit);
    }
     /**
     * 执行批量操作
     */
    protected abstract <T> void asyncAction(ActionBean<T> actionBean);

    public int offer(Object... objs) {
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


    public int offerOfWait(long timeout, TimeUnit timeUnit ,Object... objs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (true) {
            int w = this.offer(objs);
            // 尝试写入
            if (w>0) {
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

    public void add(Object... objs) {
        if (this.offer(objs) <= 0) {
            throw new RuntimeException("添加数量超过最大提交数量");
        }
    }


    protected abstract int doOffer(Object... obj);


}
