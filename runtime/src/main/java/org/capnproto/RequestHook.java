package org.capnproto;

import java.util.concurrent.CompletableFuture;

public interface RequestHook {

    RemotePromise<AnyPointer.Reader> send();

    CompletableFuture<java.lang.Void> sendStreaming();

    default Object getBrand() {
        return null;
    }
}
