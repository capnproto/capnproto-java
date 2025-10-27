package org.capnproto;

import java.util.concurrent.CompletableFuture;

public interface RequestHook {

    RemotePromise<AnyPointer.Reader> send();

    default CompletableFuture<java.lang.Void> sendStreaming() {
        return this.send().thenApply(results -> null);
    }

    default Object getBrand() {
        return null;
    }
}
