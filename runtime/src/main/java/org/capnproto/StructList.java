package org.capnproto;

public final class StructList {
    public static final class Factory<ElementBuilder, ElementReader>
        implements ListFactory<Builder<ElementBuilder>, Reader<ElementReader>> {

        public final StructFactory<ElementBuilder, ElementReader> factory;

        public Factory(StructFactory<ElementBuilder, ElementReader> factory) {
            this.factory = factory;
        }

        public final Reader<ElementReader> constructReader(SegmentReader segment,
                                                           int ptr,
                                                           int elementCount, int step,
                                                           int structDataSize, short structPointerCount,
                                                           int nestingLimit) {
            return new Reader<ElementReader>(factory,
                                             segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }

        public final Builder<ElementBuilder> constructBuilder(SegmentBuilder segment,
                                                              int ptr,
                                                              int elementCount, int step,
                                                              int structDataSize, short structPointerCount) {
            return new Builder<ElementBuilder> (factory, segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }

        public final Reader<ElementReader> fromPointerReader(PointerReader reader, SegmentReader defaultSegment, int defaultOffset) {
            return WireHelpers.readListPointer(this,
                                               reader.segment,
                                               reader.pointer,
                                               defaultSegment,
                                               defaultOffset,
                                               FieldSize.INLINE_COMPOSITE,
                                               reader.nestingLimit);
        }

        public final Builder<ElementBuilder> fromPointerBuilder(PointerBuilder builder, SegmentReader defaultSegment, int defaultOffset) {
            throw new Error();
            /*         return WireHelpers.getWritableStructListPointer(this,
                                                            builder.pointer,
                                                            builder.segment,
                                                            FieldSize.POINTER,
                                                            defaultSegment,
                                                            defaultOffset,0); */
        }

        public final Builder<ElementBuilder> initFromPointerBuilder(PointerBuilder builder,
                                                                    int elementCount) {
            return WireHelpers.initStructListPointer(this, builder.pointer, builder.segment, elementCount, factory.structSize());
        }


    }

    public static final class Reader<T> extends ListReader implements Iterable<T> {
        public final FromStructReader<T> factory;

        public Reader(FromStructReader<T> factory,
                      SegmentReader segment,
                      int ptr,
                      int elementCount, int step,
                      int structDataSize, short structPointerCount,
                      int nestingLimit) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            this.factory = factory;
        }

        public T get(int index) {
            return _getStructElement(factory, index);
        }


        public final class Iterator implements java.util.Iterator<T> {
            public Reader<T> list;
            public int idx = 0;
            public Iterator(Reader<T> list) {
                this.list = list;
            }

            public T next() {
                return list._getStructElement(factory, idx++);
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

    public static final class Builder<T> extends ListBuilder implements Iterable<T> {
        public final FromStructBuilder<T> factory;

        public Builder(FromStructBuilder<T> factory,
                       SegmentBuilder segment, int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount){
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            this.factory = factory;
        }

        public final T get(int index) {
            return _getStructElement(factory, index);
        }

        public final class Iterator implements java.util.Iterator<T> {
            public Builder<T> list;
            public int idx = 0;
            public Iterator(Builder<T> list) {
                this.list = list;
            }

            public T next() {
                return list._getStructElement(factory, idx++);
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
