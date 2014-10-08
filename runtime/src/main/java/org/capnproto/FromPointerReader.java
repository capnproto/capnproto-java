package org.capnproto;

public interface FromPointerReader<T> {
    T fromPointerReader(SegmentReader segment, int pointer, SegmentReader defaultSegment, int defaultOffset, int nestingLimit);
}
