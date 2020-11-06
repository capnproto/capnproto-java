package org.capnproto;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CompletableFutureWrapper<T> extends CompletableFuture<T> {

    private final CompletableFuture<T> other;

    public CompletableFutureWrapper(CompletionStage<T> other) {
        this.other = other.toCompletableFuture().whenComplete((value, exc) -> {
            if (exc == null) {
                this.complete(value);
            }
            else {
                this.completeExceptionally(exc);
            }
        });
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.other.cancel(mayInterruptIfRunning);
    }
}