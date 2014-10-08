package org.capnproto;

public interface FromPointerBuilderBlobDefault<T> {
    T fromPointerBuilderBlobDefault(SegmentBuilder segment, int pointer,
                                    java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize);
}
