package org.capnproto;

public interface FromPointerReaderRefDefault<T> {
    T fromPointerReaderRefDefault(SegmentReader segment, int pointer, SegmentReader defaultSegment, int defaultOffset, int nestingLimit);
}
