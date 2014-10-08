package org.capnproto;

public final class DataList {
    public static final class Factory extends ListFactory<Builder, Reader> {
        Factory() {super (FieldSize.POINTER); }
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
            return _getPointerElement(Data.factory, index);
        }

        public final class Iterator implements java.util.Iterator<Data.Reader> {
            public Reader list;
            public int idx = 0;
            public Iterator(Reader list) {
                this.list = list;
            }

            public Data.Reader next() {
                return this.list._getPointerElement(Data.factory, idx++);
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
            return _getPointerElement(Data.factory, index);
        }

        public final void set(int index, Data.Reader value) {
            _setPointerElement(Data.factory, index, value);
        }

        public final class Iterator implements java.util.Iterator<Data.Builder> {
            public Builder list;
            public int idx = 0;
            public Iterator(Builder list) {
                this.list = list;
            }

            public Data.Builder next() {
                return this.list._getPointerElement(Data.factory, idx++);
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
