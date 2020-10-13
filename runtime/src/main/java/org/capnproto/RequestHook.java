package org.capnproto;

import java.util.concurrent.CompletionStage;

public interface RequestHook {
    RemotePromise<AnyPointer.Reader> send();
    CompletionStage<?> sendStreaming();
    Object getBrand();
}
