package org.capnproto;

public interface PipelineFactory<Pipeline> {
    Pipeline newPipeline(RemotePromise<AnyPointer.Reader> promise);
}
