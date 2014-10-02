package org.capnproto;

public class EnumList {
    static <T> T clampOrdinal(T values[], short ordinal) {
        int index = ordinal;
        if (ordinal < 0 || ordinal >= values.length) {
            index = values.length - 1;
        }
        return values[index];
    }

    public static final class Reader<T> {
        public final ListReader reader;
        public final T values[];

        public Reader(ListReader reader, T values[]) {
            this.reader = reader;
            this.values = values;
        }

        public int size() {
            return this.reader.size();
        }

        public T get(int index) {
            return clampOrdinal(this.values, this.reader.getShortElement(index));
        }

    }

    public static final class Builder {

    }
}
