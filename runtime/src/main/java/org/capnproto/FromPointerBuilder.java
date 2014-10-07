package org.capnproto;

public interface FromPointerBuilder<T> {
    T constructBuilder(SegmentBuilder segment, int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount);
    T fromPointerBuilder(PointerBuilder builder, SegmentReader defaultSegment, int defaultOffset);
    T initFromPointerBuilder(PointerBuilder builder, int elementCount);
}
