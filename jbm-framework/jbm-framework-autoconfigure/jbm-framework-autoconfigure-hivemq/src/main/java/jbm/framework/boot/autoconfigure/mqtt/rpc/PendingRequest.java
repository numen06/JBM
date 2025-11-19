package jbm.framework.boot.autoconfigure.mqtt.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

/**
 * 表示一个正在等待响应的请求。
 */
class PendingRequest {

    private final String requestId;
    private final CompletableFuture<String> future = new CompletableFuture<>();
    private volatile boolean completed = false;
    private ScheduledFuture<?> timeoutFuture;

    PendingRequest(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    CompletableFuture<String> getFuture() {
        return future;
    }

    boolean isCompleted() {
        return completed;
    }

    void complete(String payload) {
        if (!completed) {
            completed = true;
            future.complete(payload);
        }
    }

    void completeExceptionally(Throwable throwable) {
        if (!completed) {
            completed = true;
            future.completeExceptionally(throwable);
        }
    }

    ScheduledFuture<?> getTimeoutFuture() {
        return timeoutFuture;
    }

    void setTimeoutFuture(ScheduledFuture<?> timeoutFuture) {
        this.timeoutFuture = timeoutFuture;
    }
}

