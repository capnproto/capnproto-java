package org.capnproto;

import java.util.concurrent.CompletableFuture;

public interface RequestHook {
    RemotePromise<AnyPointer.Reader> send();
    CompletableFuture<?> sendStreaming();
    Object getBrand();
}
