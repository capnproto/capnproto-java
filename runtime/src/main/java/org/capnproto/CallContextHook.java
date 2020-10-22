package org.capnproto;

import java.util.concurrent.CompletableFuture;

public interface CallContextHook {
    AnyPointer.Reader getParams();

    void releaseParams();

    AnyPointer.Builder getResults();

    CompletableFuture<java.lang.Void> tailCall(RequestHook request);

    void allowCancellation();

    CompletableFuture<PipelineHook> onTailCall();

    ClientHook.VoidPromiseAndPipeline directTailCall(RequestHook request);
}
