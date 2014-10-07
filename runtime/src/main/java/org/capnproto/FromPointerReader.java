package org.capnproto;

public interface FromPointerReader<T> {
    T constructReader(SegmentReader segment,
                      int ptr,
                      int elementCount, int step,
                      int structDataSize, short structPointerCount,
                      int nestingLimit);
    T fromPointerReader(PointerReader reader, SegmentReader defaultSegment, int defaultOffset);
}
