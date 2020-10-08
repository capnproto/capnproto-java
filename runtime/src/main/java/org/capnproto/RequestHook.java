package org.capnproto;

import java.util.concurrent.CompletableFuture;

interface RequestHook {
    RemotePromise<AnyPointer.Reader> send();
    CompletableFuture<?> sendStreaming();
    Object getBrand();
}
