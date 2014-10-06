package org.capnproto;

public final class StructList {
    public static final class Factory<ElementBuilder, ElementReader>
        implements ListFactory<Builder<ElementBuilder>, Reader<ElementReader>> {

        public final StructFactory<ElementBuilder, ElementReader> factory;

        public Factory(StructFactory<ElementBuilder, ElementReader> factory) {
            this.factory = factory;
        }

        public final Reader<ElementReader> fromPointerReader(PointerReader reader) {
            return new Reader<ElementReader>(factory, reader.getList(FieldSize.INLINE_COMPOSITE));
        }

        public final Builder<ElementBuilder> fromPointerBuilder(PointerBuilder builder) {
            return new Builder<ElementBuilder>(factory, builder.getStructList(this.factory.structSize()));
        }

        public final Builder<ElementBuilder> initFromPointerBuilder(PointerBuilder builder, int size) {
            return new Builder<ElementBuilder>(factory, builder.initStructList(size, this.factory.structSize()));
        }
    }

    public static final class Reader<T> implements Iterable<T> {
        public final ListReader reader;
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


        public final class Iterator implements java.util.Iterator<T> {
            public Reader<T> list;
            public int idx = 0;
            public Iterator(Reader<T> list) {
                this.list = list;
            }

            public T next() {
                return list.factory.fromStructReader(list.reader.getStructElement(idx++));
            }
            public boolean hasNext() {
                return idx < list.size();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        public java.util.Iterator<T> iterator() {
            return new Iterator(this);
        }
    }

    public static final class Builder<T> implements Iterable<T> {
        public final ListBuilder builder;
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

        public int size() {
            return this.builder.size();
        }

        public final T get(int index) {
            return this.factory.fromStructBuilder(this.builder.getStructElement(index));
        }


        public final class Iterator implements java.util.Iterator<T> {
            public Builder<T> list;
            public int idx = 0;
            public Iterator(Builder<T> list) {
                this.list = list;
            }

            public T next() {
                return list.factory.fromStructBuilder(list.builder.getStructElement(idx++));
            }
            public boolean hasNext() {
                return idx < list.size();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        public java.util.Iterator<T> iterator() {
            return new Iterator(this);
        }


    }

}
