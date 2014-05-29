package org.capnproto;

public class PrimitiveList {
    public static final class Reader<T> {
        public final ListReader reader;
        public final PrimitiveElementFactory<T> factory;

        public Reader(PrimitiveElementFactory<T> factory, ListReader reader) {
            this.factory = factory;
            this.reader = reader;
        }

        public int size() {
            return this.reader.size();
        }

        public T get(int index) {
            return this.factory.get(this.reader, index);
        }
    }

    public static final class Builder<T> {
        public final ListBuilder builder;
        public final PrimitiveElementFactory<T> factory;

        public Builder(PrimitiveElementFactory<T> factory, ListBuilder builder) {
            this.factory = factory;
            this.builder = builder;
        }


    }
}
