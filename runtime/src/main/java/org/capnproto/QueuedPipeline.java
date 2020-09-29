package org.capnproto;

import java.util.concurrent.CompletableFuture;

final class QueuedPipeline implements PipelineHook {

    final CompletableFuture<PipelineHook> promise;
    final CompletableFuture<Void> selfResolutionOp;
    PipelineHook redirect;

    public QueuedPipeline(CompletableFuture<PipelineHook> promiseParam) {
        this.promise = promiseParam.copy();
        this.selfResolutionOp = promise.handle((pipeline, exc) -> {
            this.redirect = exc == null
                    ? pipeline
                    : PipelineHook.newBrokenPipeline(exc);
            return null;
        });
    }

    @Override
    public final ClientHook getPipelinedCap(PipelineOp[] ops) {
        return redirect != null
                ? redirect.getPipelinedCap(ops)
                : new QueuedClient(this.promise.thenApply(
                    pipeline -> pipeline.getPipelinedCap(ops)));
    }
}
