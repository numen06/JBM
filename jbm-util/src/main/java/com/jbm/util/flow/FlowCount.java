package com.jbm.util.flow;

import com.jbm.util.batch.ActionBean;
import com.jbm.util.batch.RollingTask;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 计算
 *
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-07-15 19:03
 **/
public class FlowCount {

    private RollingTask<Integer> rollingTask;


    private final AtomicInteger nowFlow = new AtomicInteger(0);
    private final AtomicInteger lastFlow = new AtomicInteger(0);

    public FlowCount(long duration, TimeUnit unit) {
        rollingTask = new RollingTask<>(duration, unit, BigDecimal.ZERO.intValue(), this::init);
    }

    private Integer init(ActionBean<Integer> integerActionBean) {
        lastFlow.set(nowFlow.intValue());
        nowFlow.set(0);
        return nowFlow.get();
    }


    public int add() {
        return nowFlow.addAndGet(1);
    }

    public int nowFlow() {
        return this.nowFlow.get();
    }

    public int lastFlow() {
        return this.lastFlow.get();
    }


}
