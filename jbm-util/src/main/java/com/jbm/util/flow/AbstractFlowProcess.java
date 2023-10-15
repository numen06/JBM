package com.jbm.util.flow;

import java.util.concurrent.CompletableFuture;

public class AbstractFlowProcess<S, R> implements FlowProcess<S, R>{

    @Override
    public CompletableFuture<R> process(S source) {
        return null;
    }


}
