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
        this.response = promise.whenComplete((response, exc) -> {
           if (exc != null) {
               this.completeExceptionally(exc);
           }
           else {
               this.complete(response.getResults());
           }
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

