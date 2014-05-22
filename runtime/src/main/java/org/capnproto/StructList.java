package org.capnproto;

public final class StructList {
    public static final class Reader<T> {
        public ListReader reader;
        public final FromStructReader<T> factory;

        public Reader(FromStructReader<T> factory, ListReader reader) {
            this.reader = reader;
            this.factory = factory;
        }

        public int size() {
            return this.reader.size();
        }

        public T get(int index) {
            return this.factory.fromStructReader(this.reader.getStructElement(index));
        }
    }

    public static final class Builder<T> {
        public ListBuilder builder;
        public final FromStructBuilder<T> factory;

        public Builder(FromStructBuilder<T> factory, ListBuilder builder) {
            this.builder = builder;
            this.factory = factory;
        }

        // init
        Builder(FromStructBuilder<T> factory, PointerBuilder builder, int size) {
            this.builder = builder.initStructList(size, factory.structSize());
            this.factory = factory;
        }

        public final T get(int index) {
            return this.factory.fromStructBuilder(this.builder.getStructElement(index));
        }

    }

}
