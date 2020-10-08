package org.capnproto;

import java.util.concurrent.CompletableFuture;

public class Request<Params, Results> {

    AnyPointer.Builder params;
    private final FromPointerBuilder<Params> paramsBuilder;
    private final FromPointerReader<Results> resultsReader;
    RequestHook hook;

    Request(FromPointerBuilder<Params> paramsBuilder,
            FromPointerReader<Results> resultsReader,
            AnyPointer.Builder params, RequestHook hook) {
        this.paramsBuilder = paramsBuilder;
        this.resultsReader = resultsReader;
        this.params = params;
        this.hook = hook;
    }

    Params params() {
        return params.getAs(paramsBuilder);
    }

    CompletableFuture<Results> send() {
        var typelessPromise = hook.send();
        hook = null; // prevent reuse
        return typelessPromise.getResponse().thenApply(
                response -> response.getAs(resultsReader));
    }

    static <T, U> Request<T, U> newBrokenRequest(Throwable exc) {
        final MessageBuilder message = new MessageBuilder();

        var hook = new RequestHook() {
            @Override
            public RemotePromise<AnyPointer.Reader> send() {
                return new RemotePromise<>(CompletableFuture.failedFuture(exc), null);
            }

            @Override
            public CompletableFuture<?> sendStreaming() {
                return CompletableFuture.failedFuture(exc);
            }

            @Override
            public Object getBrand() {
                return null;
            }
        };

        var root = message.getRoot(AnyPointer.factory);
        return new Request<T, U>(null, null, root, hook);
    }

    static Request<AnyPointer.Builder, AnyPointer.Reader> newTypelessRequest(AnyPointer.Builder root, RequestHook hook) {
        return new Request<>(AnyPointer.factory, AnyPointer.factory, root, hook);
    }

    static <Params, Results> Request<Params, Results> fromTypeless(FromPointerBuilder<Params> params,
                                                                   FromPointerReader<Results> results,
                                                                   Request<AnyPointer.Builder, AnyPointer.Reader> typeless) {
        return new Request<>(params, results, typeless.params(), typeless.hook);
    }
}
