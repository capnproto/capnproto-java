package org.capnproto;

import java.util.concurrent.CompletableFuture;

public class RemotePromise<Results>
        extends CompletableFuture<Results>
        implements AutoCloseable {

    final CompletableFuture<Response<Results>> response;
    private final AnyPointer.Pipeline pipeline;

    public RemotePromise(FromPointerReader<Results> factory,
                         RemotePromise<AnyPointer.Reader> other) {
        this(other.response.thenApply(response -> Response.fromTypeless(factory, response)), other.pipeline);
    }

    public RemotePromise(CompletableFuture<Response<Results>> promise,
                         PipelineHook pipeline) {
        this(promise, new AnyPointer.Pipeline(pipeline));
    }

    public RemotePromise(CompletableFuture<Response<Results>> promise,
                         AnyPointer.Pipeline pipeline) {
        this.response = promise
                .thenApply(response -> {
                    this.complete(response.getResults());
                    return response;
                })
                .exceptionallyCompose(exc -> {
                    this.completeExceptionally(exc);
                    return CompletableFuture.failedFuture(exc);
                });
        this.pipeline = pipeline;
    }

    @Override
    public void close() {
        this.pipeline.cancel(RpcException.failed("Cancelled"));
        this.join();
    }

    public AnyPointer.Pipeline pipeline() {
        return this.pipeline;
    }
}

