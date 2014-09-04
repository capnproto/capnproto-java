package org.capnproto;

public final class DataList {
    public static final class Reader implements Iterable<Data.Reader> {
        public final ListReader reader;

        public Reader(ListReader reader) {
            this.reader = reader;
        }

        public int size() {
            return this.reader.size();
        }

        public Data.Reader get(int index) {
            return this.reader.getPointerElement(index).getData();
        }


        public final class Iterator implements java.util.Iterator<Data.Reader> {
            public Reader list;
            public int idx = 0;
            public Iterator(Reader list) {
                this.list = list;
            }

            public Data.Reader next() {
                return this.list.reader.getPointerElement(idx++).getData();
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

    public static final class Builder implements Iterable<Data.Builder> {
        public final ListBuilder builder;

        public Builder(ListBuilder builder) {
            this.builder = builder;
        }
/*
        // init
        Builder(PointerBuilder builder, int size) {
            this.builder = builder.initStructList(size, factory.structSize());
        }
*/
        public int size() {
            return this.builder.size();
        }

        public final Data.Builder get(int index) {
            return this.builder.getPointerElement(index).getData();
        }


        public final void set(int index, Data.Reader value) {
            this.builder.getPointerElement(index).setData(value);
        }

        public final class Iterator implements java.util.Iterator<Data.Builder> {
            public Builder list;
            public int idx = 0;
            public Iterator(Builder list) {
                this.list = list;
            }

            public Data.Builder next() {
                return this.list.builder.getPointerElement(idx++).getData();
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
