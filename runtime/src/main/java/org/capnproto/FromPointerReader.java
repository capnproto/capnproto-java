package org.capnproto;

public interface FromPointerReader<T> {
    T fromPointerReader(PointerReader reader, SegmentReader defaultSegment, int defaultOffset);
}
