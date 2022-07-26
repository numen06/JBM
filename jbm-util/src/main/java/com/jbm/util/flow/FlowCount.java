package com.jbm.util.flow;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 计算
 *
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-07-15 19:03
 **/
public class FlowCount {

    private final ScheduledExecutorService schePool;

    private final long duration;

    private final TimeUnit unit;

    private final AtomicInteger nowFlow = new AtomicInteger(0);

    private final AtomicInteger lastFlow = new AtomicInteger(0);

    public FlowCount(long duration, TimeUnit unit) {
        this.duration = duration;
        this.unit = unit;
        this.schePool = this.init(duration, unit);
    }

    private ScheduledExecutorService init(long duration, TimeUnit unit) {
        ScheduledExecutorService schePool = Executors.newScheduledThreadPool(2);
        schePool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                lastFlow.set(nowFlow.intValue());
                nowFlow.set(0);
            }
        }, 0, duration, unit);
        return schePool;
    }

    public int add() {
        return nowFlow.addAndGet(1);
    }

//    public int add(Long nanoTime) throws ExecutionException {
//        return this.add(new Date(nanoTime));
//    }
//
//    public int add(Date time) throws ExecutionException {
//        return flowCache.get(flag(time)).addAndGet(1);
//    }
//
//    private int flag(Long nanoTime) {
//        return flag(new Date(nanoTime));
//    }
//
//    private int flag(Date time) {
//        switch (this.unit) {
//            case MILLISECONDS:
//                return DateTime.of(time).millsecond();
//            case SECONDS:
//                return DateTime.of(time).second();
//            case MINUTES:
//                return DateTime.of(time).minute();
//            case HOURS:
//                return DateTime.of(time).hour(true);
//            case DAYS:
//                return DateTime.of(time).dayOfMonth();
//            default:
//                break;
//        }
//        return 0;
//    }


    public int nowFlow() {
        return this.nowFlow.get();
    }

    public int lastFlow() {
        return this.lastFlow.get();
    }


}
