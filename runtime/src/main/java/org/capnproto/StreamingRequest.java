package org.capnproto;


import java.util.concurrent.CompletableFuture;

public class StreamingRequest<Params> {

    private final FromPointerBuilder<Params> paramsBuilder;
    AnyPointer.Builder params;
    RequestHook hook;

    StreamingRequest(FromPointerBuilder<Params> paramsBuilder,
                     AnyPointer.Builder params, RequestHook hook) {
        this.paramsBuilder = paramsBuilder;
        this.params = params;
        this.hook = hook;
    }

    CompletableFuture<?> send() {
        var promise = hook.sendStreaming();
        hook = null; // prevent reuse
        return promise;
    }
}

