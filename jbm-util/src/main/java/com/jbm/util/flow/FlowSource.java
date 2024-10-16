package com.jbm.util.flow;

import com.ebay.bascomtask.core.TaskInterface;

import java.util.concurrent.CompletableFuture;

public interface FlowSource<R> extends TaskInterface<FlowSource> {

    CompletableFuture<R> load();

}
