package org.capnproto;

public interface InitFromPointerBuilder<T> {
    T initFromPointerBuilder(SegmentBuilder segment, int pointer);
}
