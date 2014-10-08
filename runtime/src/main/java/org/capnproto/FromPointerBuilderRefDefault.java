package org.capnproto;

public interface FromPointerBuilderRefDefault<T> {
    T fromPointerBuilderRefDefault(SegmentBuilder segment, int pointer, SegmentReader defaultSegment, int defaultOffset);
}
