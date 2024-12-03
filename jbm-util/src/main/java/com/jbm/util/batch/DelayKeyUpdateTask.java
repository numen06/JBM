package com.jbm.util.batch;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    public Consumer<Map<K, T>> commitBatchFunction;
    public Consumer<Pair<K, T>> updateFunction;
    private final Map<K, DelayBean<T>> data = new ConcurrentHashMap<>(1000);

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

    public DelayKeyUpdateTask(long delay, TimeUnit unit, Consumer<Pair<K, T>> updateFunction, Consumer<Pair<K, T>> commitFunction, Consumer<Map<K, T>> commitBatchFunction) {
        this.delay = delay;
        this.unit = unit;
        this.updateFunction = updateFunction;
        this.commitFunction = commitFunction;
        this.commitBatchFunction = commitBatchFunction;
        this.startAsync();
    }

    public DelayKeyUpdateTask(long delay, TimeUnit unit, Consumer<Pair<K, T>> updateFunction, Consumer<Pair<K, T>> commitFunction) {
        this(delay, unit, updateFunction, commitFunction, null);
    }

    public DelayKeyUpdateTask(long delay, TimeUnit unit, Consumer<Pair<K, T>> updateFunction) {
        this(delay, unit, updateFunction, null, null);
    }

    public DelayKeyUpdateTask(long delay, TimeUnit unit) {
        this(delay, unit, null, null, null);
    }


    public void awaitStop() {
        while (isRunning.get()) {
            //阻塞;
            ThreadUtil.sleep(100, TimeUnit.MILLISECONDS);
        }
    }


    public void delayUpdate(K key, T obj) {
        awaitStop();
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

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public void callCommit() {
        if (isRunning.getAndSet(true)) {
            return;
        }
        Set<K> keys = data.keySet();
        //找出需要提交的数据，处理完成后移除
        data.entrySet().stream().filter((entry) -> entry.getValue().getCounter().getAndDecrement() <= 0).forEach(e -> {
            keys.add(e.getKey());
            callCommit(e.getKey(), e.getValue());
        });
        keys.forEach(data::remove);
        //data.forEach(this::callCommit);
    }

    public void callCommit(K k, DelayBean<T> d) {
        if (commitFunction == null) {
            return;
        }
        try {
            commitFunction.accept(Pair.of(k, d.getData()));
        } catch (Exception e) {
            log.error("commit error", e);
        }
    }
}