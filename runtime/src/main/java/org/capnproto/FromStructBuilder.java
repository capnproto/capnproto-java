package org.capnproto;

public interface FromStructBuilder<T> {
    T fromStructBuilder(StructBuilder builder);
    StructSize structSize();
}
