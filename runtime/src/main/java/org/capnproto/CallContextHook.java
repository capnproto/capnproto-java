package org.capnproto;

import java.util.concurrent.CompletableFuture;

public interface CallContextHook {

    AnyPointer.Reader getParams();

    void releaseParams();

    default AnyPointer.Builder getResults() {
        return getResults(0);
    }

    AnyPointer.Builder getResults(int sizeHint);

    CompletableFuture<java.lang.Void> tailCall(RequestHook request);

    void allowCancellation();

    CompletableFuture<AnyPointer.Pipeline> onTailCall();

    ClientHook.VoidPromiseAndPipeline directTailCall(RequestHook request);

    default CompletableFuture<java.lang.Void> releaseCall() {
        return CompletableFuture.completedFuture(null);
    }
}
