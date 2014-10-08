package org.capnproto;

public interface FromPointerBuilder<T> {
    T fromPointerBuilder(SegmentBuilder segment, int pointer, SegmentReader defaultSegment, int defaultOffset);
}
