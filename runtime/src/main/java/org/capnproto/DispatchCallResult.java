package org.capnproto;

import java.util.concurrent.CompletableFuture;

public final class DispatchCallResult {

    private final CompletableFuture<?> promise;
    private final boolean streaming;

    public DispatchCallResult(CompletableFuture<?> promise, boolean isStreaming) {
        this.promise = promise;
        this.streaming = isStreaming;
    }

    public DispatchCallResult(Throwable exc) {
        this(CompletableFuture.failedFuture(exc), false);
    }

    public CompletableFuture<?> getPromise() {
        return promise;
    }

    public boolean isStreaming() {
        return streaming;
    }
}
