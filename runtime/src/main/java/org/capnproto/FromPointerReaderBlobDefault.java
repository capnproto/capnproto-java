package org.capnproto;

public interface FromPointerReaderBlobDefault<T> {
    T fromPointerReaderBlobDefault(SegmentReader segment, int pointer, java.nio.ByteBuffer defaultBuffer,
                                   int defaultOffset, int defaultSize);
}
