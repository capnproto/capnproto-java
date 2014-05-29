package org.capnproto;

public interface PrimitiveElementFactory<T> {
    public T get(ListReader listReader, int index);

    public static final PrimitiveElementFactory<Void> VOID = new PrimitiveElementFactoryVoid();
//    public static final PrimitiveElementFactory<boolean> BOOLEAN = new PrimitiveElementFactoryBoolean();
}


class PrimitiveElementFactoryVoid implements PrimitiveElementFactory<Void> {
    public Void get(ListReader listReader, int index) {
        return Void.VOID;
    }
}

/*
argh, generics must be boxed.
class PrimitiveElementFactoryBoolean implements PrimitiveElementFactory<boolean> {
    public boolean get(ListReader listReader, int index) {
        throw new Error("unimplemented");
    }
}
*/
