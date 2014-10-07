package org.capnproto;

public final class ListList {
    public static final class Factory<ElementBuilder, ElementReader>
        implements ListFactory<Builder<ElementBuilder>, Reader<ElementReader>> {

        public final ListFactory<ElementBuilder, ElementReader> factory;

        public Factory(ListFactory<ElementBuilder, ElementReader> factory) {
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

        public final Reader<ElementReader> fromPointerReader(PointerReader reader, SegmentReader defaultSegment, int defaultOffset) {
            return WireHelpers.readListPointer(this,
                                               reader.segment,
                                               reader.pointer,
                                               defaultSegment,
                                               defaultOffset,
                                               FieldSize.POINTER,
                                               reader.nestingLimit);
        }

        public final Builder<ElementBuilder> fromPointerBuilder(PointerBuilder builder, SegmentReader defaultSegment, int defaultOffset) {
            return WireHelpers.getWritableListPointer(this,
                                                      builder.pointer,
                                                      builder.segment,
                                                      FieldSize.POINTER,
                                                      defaultSegment,
                                                      defaultOffset);
        }

        public final Builder<ElementBuilder> initFromPointerBuilder(PointerBuilder builder,
                                                                    int elementCount) {
            return WireHelpers.initListPointer(this, builder.pointer, builder.segment, elementCount, FieldSize.POINTER);
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
            return this.factory.fromPointerReader(_getPointerElement(index), null, 0);
        }

    }

    public static final class Builder<T> extends ListBuilder {
        private final FromPointerBuilder<T> factory;

        public Builder(FromPointerBuilder<T> factory,
                       SegmentBuilder segment, int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount){
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            this.factory = factory;
        }

        public final T init(int index, int size) {
            return this.factory.initFromPointerBuilder(_getPointerElement(index), size);
        }

        public final T get(int index) {
            return this.factory.fromPointerBuilder(_getPointerElement(index), null, 0);
        }
    }
}
