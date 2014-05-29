package org.capnproto;

public interface PrimitiveElementFactory<T> {
    public T get(ListReader listReader, int index);

    public static final PrimitiveElementFactory<Void> VOID = new PrimitiveElementFactoryVoid();
}


class PrimitiveElementFactoryVoid implements PrimitiveElementFactory<Void> {
    public Void get(ListReader listReader, int index) {
        return Void.VOID;
    }
}

