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
public class DelayUpdateTask<T> extends AbstractScheduledService {

    private final long delay;
    private final TimeUnit unit;
    public Consumer<T> commitFunction;
    public Consumer<T> updateFunction;
    public AtomicReference<Runnable> runnable = new AtomicReference<>();
    private final AtomicInteger seconds = new AtomicInteger(1);
    private final AtomicReference<T> data = new AtomicReference<>();


    public DelayUpdateTask(long delay, TimeUnit unit, Consumer<T> updateFunction, Consumer<T> commitFunction) {
        this.delay = delay;
        this.unit = unit;
        this.updateFunction = updateFunction;
        this.commitFunction = commitFunction;
        this.startAsync();
    }

    public DelayUpdateTask(long delay, TimeUnit unit, Consumer<T> commitFunction) {
        this(delay, unit, null, commitFunction);
    }

    public DelayUpdateTask(long delay, TimeUnit unit) {
        this(delay, unit, null, null);
    }


    public void delayUpdate(T obj) {
        this.seconds.addAndGet(-1);
        if (updateFunction == null) {
            data.set(obj);
        } else {
            updateFunction.accept(data.updateAndGet(t -> obj));
        }
    }

    public void delayUpdate(T obj, Consumer<T> commitFunction) {
        this.commitFunction = commitFunction;
        this.delayUpdate(obj);
    }

    public void delayRun(Runnable runnable) {
        this.runnable = new AtomicReference<>(runnable);
        this.delayUpdate(null);
    }

    /**
     * @throws Exception
     */
    @Override
    protected void runOneIteration() throws Exception {
        this.callCommit();
    }

    /**
     * @return
     */
    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedDelaySchedule(0, delay, unit);
    }

    public void callCommit() {
        if (seconds.getAndSet(1) > 0) {
            return;
        }
        Runnable r = runnable.getAndSet(null);
        if (r != null) {
            r.run();
        }
        if (commitFunction == null) {
            return;
        }
        try {
            commitFunction.accept(data.get());
        } catch (Exception e) {
            log.error("commit error", e);
        }
    }
}