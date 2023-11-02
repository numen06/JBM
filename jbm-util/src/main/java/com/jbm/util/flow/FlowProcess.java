package com.jbm.util.flow;

import com.ebay.bascomtask.core.TaskInterface;

import java.util.concurrent.CompletableFuture;

public interface FlowProcess<S, R> extends TaskInterface<FlowProcess> {


    CompletableFuture<R> process(CompletableFuture<S> source);

    CompletableFuture<R> process(S source);

}
