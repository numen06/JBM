package com.jbm.util.batch;

import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author wesley
 */
@Slf4j
public class DelayUpdateTask<T> extends DelayKeyUpdateTask<T> {

    private final static String DEFAULT_KEY = "jbm";

    public DelayUpdateTask(long delay, TimeUnit unit, Consumer<T> updateFunction, Consumer<T> commitFunction) {
        super(delay, unit, updateFunction, commitFunction);
    }

    public void delayUpdate(T obj) {
        super.delayUpdate(DEFAULT_KEY, obj);
    }

    public void delayUpdate(T obj, Consumer<T> commitFunction) {
        super.delayUpdate(DEFAULT_KEY, obj, commitFunction);
    }

}