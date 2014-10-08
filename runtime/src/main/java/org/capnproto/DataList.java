package org.capnproto;

public final class DataList {
    public static final class Factory implements ListFactory<Builder, Reader> {
        public final Reader constructReader(SegmentReader segment,
                                              int ptr,
                                              int elementCount, int step,
                                              int structDataSize, short structPointerCount,
                                              int nestingLimit) {
            return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }

        public final Builder constructBuilder(SegmentBuilder segment,
                                              int ptr,
                                              int elementCount, int step,
                                              int structDataSize, short structPointerCount) {
            return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }

        public final Reader fromPointerReader(PointerReader reader, SegmentReader defaultSegment, int defaultOffset) {
            return WireHelpers.readListPointer(this,
                                               reader.segment,
                                               reader.pointer,
                                               defaultSegment,
                                               defaultOffset,
                                               FieldSize.POINTER,
                                               reader.nestingLimit);
        }

        public final Builder fromPointerBuilder(PointerBuilder builder, SegmentReader defaultSegment, int defaultOffset) {
            return WireHelpers.getWritableListPointer(this,
                                                      builder.pointer,
                                                      builder.segment,
                                                      FieldSize.POINTER,
                                                      defaultSegment,
                                                      defaultOffset);
        }

        public final Builder initSizedFromPointerBuilder(PointerBuilder builder, int elementCount) {
            return WireHelpers.initListPointer(this, builder.pointer, builder.segment, elementCount, FieldSize.POINTER);
        }
    }
    public static final Factory factory = new Factory();

    public static final class Reader extends ListReader implements Iterable<Data.Reader> {
        public Reader(SegmentReader segment,
                      int ptr,
                      int elementCount, int step,
                      int structDataSize, short structPointerCount,
                      int nestingLimit) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }

        public Data.Reader get(int index) {
            return _getPointerElement(index).getData();
        }

        public final class Iterator implements java.util.Iterator<Data.Reader> {
            public Reader list;
            public int idx = 0;
            public Iterator(Reader list) {
                this.list = list;
            }

            public Data.Reader next() {
                return this.list._getPointerElement(idx++).getData();
            }
            public boolean hasNext() {
                return idx < list.size();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        public java.util.Iterator<Data.Reader> iterator() {
            return new Iterator(this);
        }
    }

    public static final class Builder extends ListBuilder implements Iterable<Data.Builder> {

        public Builder(SegmentBuilder segment, int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount){
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }

        public final Data.Builder get(int index) {
            return _getPointerElement(index).getData();
        }

        public final void set(int index, Data.Reader value) {
            _getPointerElement(index).setData(value);
        }

        public final class Iterator implements java.util.Iterator<Data.Builder> {
            public Builder list;
            public int idx = 0;
            public Iterator(Builder list) {
                this.list = list;
            }

            public Data.Builder next() {
                return this.list._getPointerElement(idx++).getData();
            }
            public boolean hasNext() {
                return this.idx < this.list.size();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        public java.util.Iterator<Data.Builder> iterator() {
            return new Iterator(this);
        }


    }

}
