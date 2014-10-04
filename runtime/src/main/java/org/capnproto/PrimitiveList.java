package org.capnproto;

public class PrimitiveList {
    public static class Void {
        public static final class Factory implements ListFactory<Builder, Reader> {
            public final Reader fromPointerReader(PointerReader reader) {
                return new Reader(reader.getList(FieldSize.VOID));
            }

            public final Builder fromPointerBuilder(PointerBuilder builder) {
                return new Builder(builder.getList(FieldSize.VOID));
            }

            public final Builder initFromPointerBuilder(PointerBuilder builder, int size) {
                return new Builder(builder.initList(FieldSize.VOID, size));
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public org.capnproto.Void get(int index) {
                return org.capnproto.Void.VOID;
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }


            public int size() {
                return this.builder.size();
            }

        }
    }

    public static class Boolean {
        public static final class Factory implements ListFactory<Builder, Reader> {
            public final Reader fromPointerReader(PointerReader reader) {
                return new Reader(reader.getList(FieldSize.BIT));
            }

            public final Builder fromPointerBuilder(PointerBuilder builder) {
                return new Builder(builder.getList(FieldSize.BIT));
            }

            public final Builder initFromPointerBuilder(PointerBuilder builder, int size) {
                return new Builder(builder.initList(FieldSize.BIT, size));
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public boolean get(int index) {
                return this.reader.getBooleanElement(index);
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

            public int size() {
                return this.builder.size();
            }

            public boolean get(int index) {
                return this.builder.getBooleanElement(index);
            }

            public void set(int index, boolean value) {
                this.builder.setBooleanElement(index, value);
            }
        }
    }

    public static class Byte {
        public static final class Factory implements ListFactory<Builder, Reader> {
            public final Reader fromPointerReader(PointerReader reader) {
                return new Reader(reader.getList(FieldSize.BYTE));
            }

            public final Builder fromPointerBuilder(PointerBuilder builder) {
                return new Builder(builder.getList(FieldSize.BYTE));
            }

            public final Builder initFromPointerBuilder(PointerBuilder builder, int size) {
                return new Builder(builder.initList(FieldSize.BYTE, size));
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public byte get(int index) {
                return this.reader.getByteElement(index);
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

            public int size() {
                return this.builder.size();
            }

            public byte get(int index) {
                return this.builder.getByteElement(index);
            }

            public void set(int index, byte value) {
                this.builder.setByteElement(index, value);
            }
        }

    }

    public static class Short {
        public static final class Factory implements ListFactory<Builder, Reader> {
            public final Reader fromPointerReader(PointerReader reader) {
                return new Reader(reader.getList(FieldSize.TWO_BYTES));
            }

            public final Builder fromPointerBuilder(PointerBuilder builder) {
                return new Builder(builder.getList(FieldSize.TWO_BYTES));
            }

            public final Builder initFromPointerBuilder(PointerBuilder builder, int size) {
                return new Builder(builder.initList(FieldSize.TWO_BYTES, size));
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public short get(int index) {
                return this.reader.getShortElement(index);
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

            public int size() {
                return this.builder.size();
            }

            public short get(int index) {
                return this.builder.getShortElement(index);
            }

            public void set(int index, short value) {
                this.builder.setShortElement(index, value);
            }
        }

    }

    public static class Int {
        public static final class Factory implements ListFactory<Builder, Reader> {
            public final Reader fromPointerReader(PointerReader reader) {
                return new Reader(reader.getList(FieldSize.FOUR_BYTES));
            }

            public final Builder fromPointerBuilder(PointerBuilder builder) {
                return new Builder(builder.getList(FieldSize.FOUR_BYTES));
            }

            public final Builder initFromPointerBuilder(PointerBuilder builder, int size) {
                return new Builder(builder.initList(FieldSize.FOUR_BYTES, size));
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public int get(int index) {
                return this.reader.getIntElement(index);
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

            public int size() {
                return this.builder.size();
            }

            public int get(int index) {
                return this.builder.getIntElement(index);
            }

            public void set(int index, int value) {
                this.builder.setIntElement(index, value);
            }
        }
    }

    public static class Float {
        public static final class Factory implements ListFactory<Builder, Reader> {
            public final Reader fromPointerReader(PointerReader reader) {
                return new Reader(reader.getList(FieldSize.FOUR_BYTES));
            }

            public final Builder fromPointerBuilder(PointerBuilder builder) {
                return new Builder(builder.getList(FieldSize.FOUR_BYTES));
            }

            public final Builder initFromPointerBuilder(PointerBuilder builder, int size) {
                return new Builder(builder.initList(FieldSize.FOUR_BYTES, size));
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public float get(int index) {
                return this.reader.getFloatElement(index);
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

            public int size() {
                return this.builder.size();
            }

            public float get(int index) {
                return this.builder.getFloatElement(index);
            }

            public void set(int index, float value) {
                this.builder.setFloatElement(index, value);
            }
        }
    }


    public static class Long {
        public static final class Factory implements ListFactory<Builder, Reader> {
            public final Reader fromPointerReader(PointerReader reader) {
                return new Reader(reader.getList(FieldSize.EIGHT_BYTES));
            }

            public final Builder fromPointerBuilder(PointerBuilder builder) {
                return new Builder(builder.getList(FieldSize.EIGHT_BYTES));
            }

            public final Builder initFromPointerBuilder(PointerBuilder builder, int size) {
                return new Builder(builder.initList(FieldSize.EIGHT_BYTES, size));
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public long get(int index) {
                return this.reader.getLongElement(index);
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

            public int size() {
                return this.builder.size();
            }

            public long get(int index) {
                return this.builder.getLongElement(index);
            }

            public void set(int index, long value) {
                this.builder.setLongElement(index, value);
            }
        }
    }

    public static class Double {
        public static final class Factory implements ListFactory<Builder, Reader> {
            public final Reader fromPointerReader(PointerReader reader) {
                return new Reader(reader.getList(FieldSize.EIGHT_BYTES));
            }

            public final Builder fromPointerBuilder(PointerBuilder builder) {
                return new Builder(builder.getList(FieldSize.EIGHT_BYTES));
            }

            public final Builder initFromPointerBuilder(PointerBuilder builder, int size) {
                return new Builder(builder.initList(FieldSize.EIGHT_BYTES, size));
            }
        }
        public static final Factory factory = new Factory();

        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public double get(int index) {
                return this.reader.getDoubleElement(index);
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

            public int size() {
                return this.builder.size();
            }

            public double get(int index) {
                return this.builder.getDoubleElement(index);
            }

            public void set(int index, double value) {
                this.builder.setDoubleElement(index, value);
            }
        }
    }
}
