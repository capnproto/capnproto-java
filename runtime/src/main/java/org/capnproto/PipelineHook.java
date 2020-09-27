package org.capnproto;

interface PipelineHook {

    ClientHook getPipelinedCap(PipelineOp[] ops);

    static PipelineHook newBrokenPipeline(Throwable exc) {
        return ops -> ClientHook.newBrokenCap(exc);
    }
}
