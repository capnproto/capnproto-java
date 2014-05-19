package org.capnproto;

public interface FromStructReader<T> {
    T fromStructReader(StructReader reader);
}
