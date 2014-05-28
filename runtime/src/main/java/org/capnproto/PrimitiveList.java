package org.capnproto;

public class PrimitiveList {
    public static final class Reader<T> {
        public final ListReader reader;

        public Reader(ListReader reader) {
            this.reader = reader;
        }

        public int size() {
            return this.reader.size();
        }

        public T get(int index) {
            throw new Error();
        }
    }

    public static final class Builder<T> {

    }
}
