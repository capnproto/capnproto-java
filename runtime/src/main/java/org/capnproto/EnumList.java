package org.capnproto;

public class EnumList {
    static <T> T clampOrdinal(T values[], short ordinal) {
        int index = ordinal;
        if (ordinal < 0 || ordinal >= values.length) {
            index = values.length - 1;
        }
        return values[index];
    }

    public static final class Factory<T extends java.lang.Enum> {
        public final T values[];

        public Factory(T values[]) {
            this.values = values;
        }

        public final Reader<T> fromPointerReader(PointerReader reader) {
            return new Reader<T>(values, reader.getList(FieldSize.TWO_BYTES));
        }

        public final Builder<T> fromPointerBuilder(PointerBuilder builder) {
            return new Builder<T>(values, builder.getList(FieldSize.TWO_BYTES));
        }

        public final Builder<T> initFromPointerBuilder(PointerBuilder builder, int size) {
            return new Builder<T>(values, builder.initList(FieldSize.TWO_BYTES, size));
        }
    }

    public static final class Reader<T extends java.lang.Enum> {
        public final ListReader reader;
        public final T values[];

        public Reader(T values[], ListReader reader) {
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

    public static final class Builder<T extends java.lang.Enum> {
        public final ListBuilder builder;
        public final T values[];

        public Builder(T values[], ListBuilder builder) {
            this.builder = builder;
            this.values = values;
        }

        public int size() {
            return this.builder.size();
        }

        public T get(int index) {
            return clampOrdinal(this.values, this.builder.getShortElement(index));
        }

        public void set(int index, T value) {
            this.builder.setShortElement(index, (short)value.ordinal());
        }
    }
}
