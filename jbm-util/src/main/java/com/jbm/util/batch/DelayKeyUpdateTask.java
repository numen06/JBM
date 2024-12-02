package com.jbm.util.batch;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author wesley
 */
@Slf4j
public class DelayKeyUpdateTask<K extends Serializable, T> extends AbstractScheduledService {

    private final long delay;
    private final TimeUnit unit;
    public Consumer<Pair<K, T>> commitFunction;
    public Consumer<Pair<K, T>> updateFunction;
    private final Map<K, DelayBean<T>> data = new ConcurrentHashMap<>();

    @Getter
    public static class DelayBean<D> implements Serializable {
        private final AtomicInteger counter = new AtomicInteger(1);
        private D data;

        public DelayBean(D data) {
            this.data = data;
        }

        public DelayBean(D data, int counter) {
            this.data = data;
            this.counter.set(counter);
        }

        public void decrementAndGet(D data) {
            this.counter.decrementAndGet();
            this.data = data;
        }
    }

    public DelayKeyUpdateTask(long delay, TimeUnit unit, Consumer<Pair<K, T>> updateFunction, Consumer<Pair<K, T>> commitFunction) {
        this.delay = delay;
        this.unit = unit;
        this.updateFunction = updateFunction;
        this.commitFunction = commitFunction;
        this.startAsync();
    }

    public DelayKeyUpdateTask(long delay, TimeUnit unit) {
        this(delay, unit, null, null);
    }


    public void delayUpdate(K key, T obj) {
        this.data.putIfAbsent(key, new DelayBean<>(obj));
        DelayBean<T> delayBean = data.get(key);
        delayBean.decrementAndGet(obj);
        if (updateFunction != null) {
            updateFunction.accept(Pair.of(key, delayBean.getData()));
        }
    }


    public void delayUpdate(Supplier<Pair<K, T>> supplier, Consumer<Pair<K, T>> commitFunction) {
        Pair<K, T> pair = supplier.get();
        this.delayUpdate(pair.getKey(), pair.getValue(), null, commitFunction);
    }


    public void delayUpdate(K key, T obj, Consumer<Pair<K, T>> commitFunction) {
        this.delayUpdate(() -> Pair.of(key, obj), commitFunction);
    }

    public void delayUpdate(K key, T obj, Consumer<Pair<K, T>> updateFunction, Consumer<Pair<K, T>> commitFunction) {
        if (ObjectUtil.isNotNull(commitFunction)) {
            this.commitFunction = commitFunction;
        }
        if (ObjectUtil.isNotNull(updateFunction)) {
            this.updateFunction = updateFunction;
        }
        this.delayUpdate(key, obj);
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
        data.entrySet().parallelStream().forEach(e -> this.callCommit(e.getKey(), e.getValue()));
    }

    public void callCommit(K k, DelayBean<T> d) {
        if (d.getCounter().getAndSet(1) > 0) {
            return;
        }
        if (commitFunction == null) {
            return;
        }
        try {
            commitFunction.accept(Pair.of(k, d.getData()));
        } catch (Exception e) {
            log.error("commit error", e);
        } finally {
            data.remove(d);
        }
    }
}