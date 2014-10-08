package org.capnproto;

public final class ListList {
    public static final class Factory<ElementBuilder, ElementReader>
        extends ListFactory<Builder<ElementBuilder>, Reader<ElementReader>> {

        public final ListFactory<ElementBuilder, ElementReader> factory;

        public Factory(ListFactory<ElementBuilder, ElementReader> factory) {
            super(FieldSize.POINTER);
            this.factory = factory;
        }

        public final Reader<ElementReader> constructReader(SegmentReader segment,
                                                             int ptr,
                                                             int elementCount, int step,
                                                             int structDataSize, short structPointerCount,
                                                             int nestingLimit) {
            return new Reader<ElementReader>(factory, segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }

        public final Builder<ElementBuilder> constructBuilder(SegmentBuilder segment,
                                                              int ptr,
                                                              int elementCount, int step,
                                                              int structDataSize, short structPointerCount) {
            return new Builder<ElementBuilder>(factory, segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }
    }

    public static final class Reader<T> extends ListReader {
        private final FromPointerReader<T> factory;

        public Reader(FromPointerReader<T> factory,
                      SegmentReader segment,
                      int ptr,
                      int elementCount, int step,
                      int structDataSize, short structPointerCount,
                      int nestingLimit) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            this.factory = factory;
        }

        public T get(int index) {
            return _getPointerElement(this.factory, index, null, 0);
        }

    }

    public static final class Builder<T> extends ListBuilder {
        private final ListFactory<T, ?> factory;

        public Builder(ListFactory<T, ?> factory,
                       SegmentBuilder segment, int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount){
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            this.factory = factory;
        }

        public final T init(int index, int size) {
            return this.factory.initSizedFromPointerBuilder(_getPointerElement(index), size);
        }

        public final T get(int index) {
            return _getPointerElement(this.factory, index, null, 0);
        }
    }
}
