package org.capnproto;

public interface InitSizedFromPointerBuilder<T> {
    T initSizedFromPointerBuilder(PointerBuilder builder, int elementCount);
}
