package org.capnproto;

public interface FromStructReader<T> {
    public abstract T fromStructReader(StructReader reader);
}
