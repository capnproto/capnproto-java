package org.capnproto;

public interface FromStructReader<T> {
    T fromStructReader(SegmentReader segment, int data, int pointers,
                       int dataSize, short pointerCount,
                       byte bit0Offset, int nestingLimit);
}
