package org.capnproto;

public interface PipelineHook {

    ClientHook getPipelinedCap(short[] ops);

    default void cancel(Throwable exc) {
    }

    static PipelineHook newBrokenPipeline(Throwable exc) {
        return ops -> Capability.newBrokenCap(exc);
    }
}
