package org.capnproto;

import java.util.concurrent.CompletableFuture;

public class RemotePromise<Results>
        extends CompletableFutureWrapper<Results> {

    private final CompletableFuture<Response<Results>> response;
    private final AnyPointer.Pipeline pipeline;

    public RemotePromise(FromPointerReader<Results> factory,
                         RemotePromise<AnyPointer.Reader> other) {
        super(other.thenApply(response -> response.getAs(factory)));
        this.response = other.response.thenApply(
                response -> new Response<>(
                        response.getResults().getAs(factory),
                        response.getHook()));
        this.pipeline = other.pipeline;
    }

    public RemotePromise(CompletableFuture<Response<Results>> promise,
                         AnyPointer.Pipeline pipeline) {
        super(promise.thenApply(response -> {
            //System.out.println("Got a response for remote promise " + promise.toString());
            return response.getResults();
        }));
        this.response = promise;
        this.pipeline = pipeline;
    }

    public AnyPointer.Pipeline pipeline() {
        return this.pipeline;
    }

    public static <R> RemotePromise<R> fromTypeless(
            FromPointerReader<R> resultsFactory,
            RemotePromise<AnyPointer.Reader> typeless) {
        var promise = typeless.response.thenApply(
                response -> Response.fromTypeless(resultsFactory, response));
        return new RemotePromise<>(promise, typeless.pipeline);
    }
}

