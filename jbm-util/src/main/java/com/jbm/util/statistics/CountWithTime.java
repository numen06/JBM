package com.jbm.util.statistics;

import com.google.common.util.concurrent.AbstractScheduledService;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 一段时间内的计数统计
 */
public abstract class CountWithTime extends AbstractScheduledService {

    private AtomicLong total = new AtomicLong(0);
    private long start = 0;
    private long end = 0;
    private long avg = 0;

    public CountWithTime() {
        this(0L);
    }


//    protected abstract long period();
//
//    protected abstract TimeUnit unit();

    public CountWithTime(long count) {
        this.total = new AtomicLong(count);
        this.startAsync();
        this.awaitRunning();
    }

    public long add() {
        return total.addAndGet(1);
    }

    public long getTotal() {
        return this.total.get();
    }

    @Override
    protected void runOneIteration() throws Exception {
        if (start == 0) {
            start = total.get();
        }
        if (end == 0) {
            end = total.get();
        }
        if (end > 0 && start > 0) {
            avg = end - start;
            start = end;
            end = 0;
        }
        try {
            print();
        } catch (Exception e) {

        }
    }

    /**
     * 周期内打印
     */
    public abstract void print();

    /***
     * 获取周期内的平均值
     * @return
     */
    public long getAvg() {
        return this.avg;
    }

}
