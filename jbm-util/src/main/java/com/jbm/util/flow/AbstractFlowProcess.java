package com.jbm.util.flow;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class AbstractFlowProcess<S, R> implements FlowProcess<S, R> {

    @Override
    public CompletableFuture<R> process(CompletableFuture<S> source) {
        try {
            return this.process(source.get());
        } catch (Exception e) {
            log.error("流程处理异常", e);
            throw new RuntimeException("流程处理异常", e);
        }

    }

    @Override
    public CompletableFuture<R> process(S source) {
        return complete(doProcess(source));
    }


    public abstract R doProcess(S source);

    public CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs) {
        return CompletableFuture.anyOf(cfs);
    }

    public CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
        return CompletableFuture.allOf(cfs);
    }


}
