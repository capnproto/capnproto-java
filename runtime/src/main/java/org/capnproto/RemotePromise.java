package org.capnproto;

import java.util.concurrent.CompletableFuture;

class RemotePromise<Results> {

    final CompletableFuture<Response<Results>> response;
    final PipelineHook pipeline;

    RemotePromise(CompletableFuture<Response<Results>> response,
                  PipelineHook pipeline) {
        this.response = response;
        this.pipeline = pipeline;
    }


    public CompletableFuture<Response<Results>> getResponse() {
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
