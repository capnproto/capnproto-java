package org.capnproto;

public interface FromPointerBuilder<T> {
    T fromPointerBuilder(PointerBuilder builder);
    T initFromPointerBuilder(PointerBuilder value, int size);
}
