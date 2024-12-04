package com.jbm.util.batch;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author wesley
 */
@Slf4j
public class DelayUpdateTask<T> extends DelayKeyUpdateTask<String, T> {

//    private final static String DEFAULT_KEY = "jbm";

    private final String defaultKey = IdUtil.fastSimpleUUID();

    public DelayUpdateTask(long delay, TimeUnit unit, Consumer<Pair<String, T>> updateFunction, Consumer<Pair<String, T>> commitFunction) {
        super(delay, unit, updateFunction, commitFunction);
    }

    public void delayUpdate(T obj) {
        super.delayUpdate(defaultKey, obj);
    }

    public void delayUpdate(T obj, Consumer<Pair<String, T>> commitFunction) {
        super.delayUpdate(defaultKey, obj, commitFunction);
    }

}