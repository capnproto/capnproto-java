package org.capnproto;

import java.util.concurrent.CompletableFuture;

public class Request<Params, Results> {

    final AnyPointer.Builder params;
    private final FromPointerReader<Results> results;
    RequestHook hook;

    Request(AnyPointer.Builder params, FromPointerReader<Results> results, RequestHook hook) {
        this.params = params;
        this.results = results;
        this.hook = hook;
    }

    AnyPointer.Builder params() {
        return params;
    }

    CompletableFuture<Results> send() {
        var typelessPromise = hook.send();
        hook = null; // prevent reuse
        return typelessPromise.getResponse().thenApply(response -> {
            return response.getAs(results);
        });
    }

    static <T, U> Request<T, U> newBrokenRequest(Throwable exc) {
        final MessageBuilder message = new MessageBuilder();

        var hook = new RequestHook() {
            @Override
            public RemotePromise<AnyPointer.Reader> send() {
                return new RemotePromise<>(CompletableFuture.failedFuture(exc), null);
            }

            @Override
            public Object getBrand() {
                return null;
            }
        };

        var root = message.getRoot(AnyPointer.factory);
        return new Request<T, U>(root, null, hook);
    }
}

