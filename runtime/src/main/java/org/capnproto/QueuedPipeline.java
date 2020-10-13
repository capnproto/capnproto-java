package org.capnproto;

import java.util.concurrent.CompletionStage;

final class QueuedPipeline implements PipelineHook {

    final CompletionStage<PipelineHook> promise;
    final CompletionStage<Void> selfResolutionOp;
    PipelineHook redirect;

    public QueuedPipeline(CompletionStage<PipelineHook> promiseParam) {
        this.promise = promiseParam;
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
