package org.capnproto;

import java.util.concurrent.CompletableFuture;

class RemotePromise<Results> {

    final CompletableFuture<Response> response;
    final PipelineHook pipeline;

    RemotePromise(CompletableFuture<Response> response, PipelineHook pipeline) {
        this.response = response;
        this.pipeline = pipeline;
    }

    public CompletableFuture<Response> getResponse() {
        return response;
    }

    public CompletableFuture<?> ignoreResult() {
        return this.response.thenCompose(
                result -> CompletableFuture.completedFuture(null));
    }

    public PipelineHook getHook() {
        return pipeline;
    }
}
