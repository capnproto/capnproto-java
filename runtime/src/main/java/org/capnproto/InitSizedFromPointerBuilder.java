package org.capnproto;

public interface InitSizedFromPointerBuilder<T> {
    T initSizedFromPointerBuilder(SegmentBuilder segment, int pointer, int elementCount);
}
