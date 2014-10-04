package org.capnproto;

public final class ListList {
    public static final class Factory<ElementReader, ElementBuilder> implements ListFactory<Reader<ElementReader>,
                                                               Builder<ElementBuilder>> {
        public final ListFactory<ElementReader, ElementBuilder> factory;

        public Factory(ListFactory<ElementReader, ElementBuilder> factory) {
            this.factory = factory;
        }

        public final Reader<ElementReader> fromPointerReader(PointerReader reader) {
            return new Reader<ElementReader>(factory, reader.getList(FieldSize.POINTER));
        }

        public final Builder<ElementBuilder> fromPointerBuilder(PointerBuilder builder) {
            return new Builder<ElementBuilder>(factory, builder.getList(FieldSize.POINTER));
        }

        public final Builder<ElementBuilder> initFromPointerBuilder(PointerBuilder builder, int size) {
            return new Builder<ElementBuilder>(factory, builder.initList(FieldSize.POINTER, size));
        }
    }


    public static final class Reader<T> {
        public final ListReader reader;
        private final FromPointerReader<T> factory;

        public Reader(FromPointerReader<T> factory, ListReader reader) {
            this.factory = factory;
            this.reader = reader;
        }

        public final int size() {
            return this.reader.size();
        }

        public T get(int index) {
            return this.factory.fromPointerReader(this.reader.getPointerElement(index));
        }

    }

    public static final class Builder<T> {
        public final ListBuilder builder;
        private final FromPointerBuilder<T> factory;

        public Builder(FromPointerBuilder<T> factory, ListBuilder builder) {
            this.factory = factory;
            this.builder = builder;
        }

        public final int size() {
            return this.builder.size();
        }

        public final T init(int index, int size) {
            return this.factory.initFromPointerBuilder(this.builder.getPointerElement(index), size);
        }

        public final T get(int index) {
            return this.factory.fromPointerBuilder(this.builder.getPointerElement(index));
        }
    }
}
