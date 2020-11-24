package org.capnproto;

import java.util.concurrent.CompletionStage;

public interface RequestHook {

    RemotePromise<AnyPointer.Reader> send();

    CompletionStage<?> sendStreaming();

    default Object getBrand() {
        return null;
    }
}
