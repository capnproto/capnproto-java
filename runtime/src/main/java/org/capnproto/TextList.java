package org.capnproto;

public final class TextList {
    public static final class Reader implements Iterable<Text.Reader> {
        public final ListReader reader;

        public Reader(ListReader reader) {
            this.reader = reader;
        }

        public int size() {
            return this.reader.size();
        }

        public Text.Reader get(int index) {
            return this.reader.getPointerElement(index).getText();
        }


        public final class Iterator implements java.util.Iterator<Text.Reader> {
            public Reader list;
            public int idx = 0;
            public Iterator(Reader list) {
                this.list = list;
            }

            public Text.Reader next() {
                return this.list.reader.getPointerElement(idx++).getText();
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

    public static final class Builder implements Iterable<Text.Builder> {
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

        public final Text.Builder get(int index) {
            return this.builder.getPointerElement(index).getText();
        }


        public final class Iterator implements java.util.Iterator<Text.Builder> {
            public Builder list;
            public int idx = 0;
            public Iterator(Builder list) {
                this.list = list;
            }

            public Text.Builder next() {
                return this.list.builder.getPointerElement(idx++).getText();
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
