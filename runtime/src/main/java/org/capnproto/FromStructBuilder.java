package org.capnproto;

public interface FromStructBuilder<T> {
    T fromStructBuilder(SegmentBuilder segment, int data, int pointers, int dataSize,
                        short pointerCount, byte bit0Offset);
    StructSize structSize();
}
