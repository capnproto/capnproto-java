package org.capnproto;

public class PrimitiveList {
    public static class Void {
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
        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public boolean get(int index) {
                throw new Error();
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



    public static class Byte {
        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public byte get(int index) {
                throw new Error();
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

        }

    }

    public static class Short {
        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public short get(int index) {
                throw new Error();
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

        }

    }

    public static class Int {
        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public int get(int index) {
                throw new Error();
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

        }

    }

    public static class Float {
        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public float get(int index) {
                throw new Error();
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

        }

    }


    public static class Long {
        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public long get(int index) {
                throw new Error();
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

        }

    }

    public static class Double {
        public static final class Reader {
            public final ListReader reader;

            public Reader(ListReader reader) {
                this.reader = reader;
            }

            public int size() {
                return this.reader.size();
            }

            public double get(int index) {
                throw new Error();
            }
        }

        public static final class Builder {
            public final ListBuilder builder;

            public Builder(ListBuilder builder) {
                this.builder = builder;
            }

        }

    }


}
