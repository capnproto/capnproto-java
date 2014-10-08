package org.capnproto;

public final class TextList {
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

    public static final class Reader extends ListReader implements Iterable<Text.Reader> {
        public Reader(SegmentReader segment,
                      int ptr,
                      int elementCount, int step,
                      int structDataSize, short structPointerCount,
                      int nestingLimit) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }

        public Text.Reader get(int index) {
            return _getPointerElement(Text.factory, index, null, 0, 0);
        }

        public final class Iterator implements java.util.Iterator<Text.Reader> {
            public Reader list;
            public int idx = 0;
            public Iterator(Reader list) {
                this.list = list;
            }

            public Text.Reader next() {
                return _getPointerElement(Text.factory, idx++, null, 0, 0);
            }
            public boolean hasNext() {
                return idx < list.size();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        public java.util.Iterator<Text.Reader> iterator() {
            return new Iterator(this);
        }

    }

    public static final class Builder extends ListBuilder implements Iterable<Text.Builder> {
        public Builder(SegmentBuilder segment, int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount){
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }

        public final Text.Builder get(int index) {
            return _getPointerElement(Text.factory, index, null, 0, 0);
        }

        public final void set(int index, Text.Reader value) {
            _getPointerElement(index).setText(value);
        }

        public final class Iterator implements java.util.Iterator<Text.Builder> {
            public Builder list;
            public int idx = 0;
            public Iterator(Builder list) {
                this.list = list;
            }

            public Text.Builder next() {
                return this.list._getPointerElement(Text.factory, idx++, null, 0, 0);
            }
            public boolean hasNext() {
                return this.idx < this.list.size();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        public java.util.Iterator<Text.Builder> iterator() {
            return new Iterator(this);
        }
    }
}
