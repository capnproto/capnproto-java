package org.capnproto;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class RemotePromise<Results>
        extends CompletableFutureWrapper<Results> {

    final CompletableFuture<Response<Results>> response;
    final PipelineHook hook;

    RemotePromise(CompletableFuture<Response<Results>> promise,
                  PipelineHook hook) {
        super(promise.thenApply(response -> response.getResults()));
        this.response = promise;
        this.hook = hook;
    }

    public static <R> RemotePromise<R> fromTypeless(
            FromPointerReader<R> resultsFactory,
            RemotePromise<AnyPointer.Reader> typeless) {
        var promise = typeless.response.thenApply(
                response -> Response.fromTypeless(resultsFactory, response));
        return new RemotePromise<>(promise, typeless.hook);
    }
}

