package com.jbm.util.flow;

import com.ebay.bascomtask.core.TaskInterface;

import java.util.concurrent.CompletableFuture;

public interface FlowOutput<S> extends TaskInterface<FlowOutput> {

    CompletableFuture<Void> output(CompletableFuture<S> source);

    CompletableFuture<Void> output(S source);

}
