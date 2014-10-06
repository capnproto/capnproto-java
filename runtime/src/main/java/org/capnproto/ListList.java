package org.capnproto;

public final class ListList {
    public static final class Factory<ElementBuilder, ElementReader>
        implements ListFactory<Builder<ElementBuilder>, Reader<ElementReader>> {

        public final ListFactory<ElementBuilder, ElementReader> factory;

        public Factory(ListFactory<ElementBuilder, ElementReader> factory) {
            this.factory = factory;
        }

        public final Reader<ElementReader> fromPointerReader(PointerReader reader, SegmentReader defaultSegment, int defaultOffset) {
            return new Reader<ElementReader>(factory, reader.getList(FieldSize.POINTER, defaultSegment, defaultOffset));
        }

        public final Builder<ElementBuilder> fromPointerBuilder(PointerBuilder builder, SegmentReader defaultSegment, int defaultOffset) {
            return new Builder<ElementBuilder>(factory, builder.getList(FieldSize.POINTER, defaultSegment, defaultOffset));
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
            return this.factory.fromPointerReader(this.reader.getPointerElement(index), null, 0);
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
            return this.factory.fromPointerBuilder(this.builder.getPointerElement(index), null, 0);
        }
    }
}
