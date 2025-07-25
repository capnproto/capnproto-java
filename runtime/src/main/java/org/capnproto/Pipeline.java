package org.capnproto;

public interface Pipeline {

    AnyPointer.Pipeline typelessPipeline();

    default void cancel(Throwable exc) {
        this.typelessPipeline().cancel(exc);
    }
}
