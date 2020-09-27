package org.capnproto;

import java.util.concurrent.CompletableFuture;

public class Request<Params, Results> {

    private final AnyPointer.Builder params;
    private final RequestHook hook;

    Request(AnyPointer.Builder params, RequestHook hook) {
        this.params = params;
        this.hook = hook;
    }

    AnyPointer.Builder params() {
        return params;
    }

    CompletableFuture<Results> send() {
        return null;
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
        return new Request<T, U>(root, hook);
    }
}

