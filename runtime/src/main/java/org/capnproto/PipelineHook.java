package org.capnproto;

public interface PipelineHook extends AutoCloseable {

    ClientHook getPipelinedCap(PipelineOp[] ops);

    static PipelineHook newBrokenPipeline(Throwable exc) {
        return ops -> Capability.newBrokenCap(exc);
    }

    @Override
    default void close() {
    }
}
