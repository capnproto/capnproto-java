package org.capnproto;

public interface FromPointerReader<T> {
    T fromPointerReader(SegmentReader segment, int pointer, int nestingLimit);
}
