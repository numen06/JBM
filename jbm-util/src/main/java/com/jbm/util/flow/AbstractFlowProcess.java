package com.jbm.util.flow;

import com.ebay.bascomtask.core.Orchestrator;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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


}
