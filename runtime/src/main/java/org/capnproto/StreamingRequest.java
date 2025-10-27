package org.capnproto;

import java.util.concurrent.CompletableFuture;

public interface StreamingRequest<Params> {

    FromPointerBuilder<Params> getParamsFactory();

    StreamingRequest<AnyPointer.Builder> getTypelessRequest();

    default Params getParams() {
        return this.getTypelessRequest().getParams().getAs(this.getParamsFactory());
    }

    default RequestHook getHook() {
        return this.getTypelessRequest().getHook();
    }

    default CompletableFuture<java.lang.Void> send() {
        return this.getHook().sendStreaming();
    }
}

