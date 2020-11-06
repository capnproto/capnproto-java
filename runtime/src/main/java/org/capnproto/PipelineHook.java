package org.capnproto;

public interface PipelineHook extends AutoCloseable {

    ClientHook getPipelinedCap(PipelineOp[] ops);

    static PipelineHook newBrokenPipeline(Throwable exc) {
        return new PipelineHook() {
            @Override
            public ClientHook getPipelinedCap(PipelineOp[] ops) {
                return Capability.newBrokenCap(exc);
            }
        };
    }

    @Override
    default void close() {
    }
}
