package org.capnproto;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CompletableFutureWrapper<T> extends CompletableFuture<T> {

    public CompletableFutureWrapper(CompletionStage<T> other) {
        other.toCompletableFuture().whenComplete((value, exc) -> {
            if (exc == null) {
                this.complete(value);
            }
            else {
                this.completeExceptionally(exc);
            }
        });
    }
}