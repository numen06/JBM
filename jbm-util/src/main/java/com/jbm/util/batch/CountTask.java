package com.jbm.util.batch;

import com.google.common.util.concurrent.AbstractScheduledService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


/**
 * 计数任务执行器
 */
public class CountTask extends BatchTask<Long> {


    public CountTask(Consumer action) {
        super(action);
    }

    public CountTask(Long maxSubmitTime, TimeUnit timeUnit, Integer maxSubmitQuantity, Consumer action) {
        super(maxSubmitTime, timeUnit, maxSubmitQuantity, action);
    }
}
