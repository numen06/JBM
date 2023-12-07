package com.jbm.util.flow;

import java.util.concurrent.CompletableFuture;

public class FlowFactory {

    public <T> CompletableFuture<T> createCompletableFuture(T source) {
        return CompletableFuture.supplyAsync(() -> source);
    }

    public <T> CompletableFuture<T> completableFuture(T source) {
        return CompletableFuture.supplyAsync(() -> source);
    }


}
