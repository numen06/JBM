package com.jbm.util.flow;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class AbstractFlowSource<R> implements FlowSource<R> {


    public abstract R doLoad();

    /**
     * @return
     */
    @Override
    public CompletableFuture<R> load() {
        try {
            return this.load();
        } catch (Exception e) {
            log.error("加载流程异常", e);
            throw new RuntimeException("加载流程异常", e);
        }
    }
}
