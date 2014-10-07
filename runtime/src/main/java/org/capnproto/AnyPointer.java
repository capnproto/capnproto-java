package org.capnproto;

public final class AnyPointer {

    public final static class Reader {
        public final PointerReader reader;

        public Reader(PointerReader reader) {
            this.reader = reader;
        }

        public final <T> T getAsStruct(FromStructReader<T> factory) {
            return this.reader.getStruct(factory);
        }

        public final <T> T getAsList(FromPointerReader<T> factory) {
            return factory.fromPointerReader(this.reader, null, 0);
        }
    }

    public static final class Builder {
        public final PointerBuilder builder;

        public Builder(PointerBuilder builder) {
            this.builder = builder;
        }

        public final <T> T initAsStruct(FromStructBuilder<T> factory) {
            return this.builder.initStruct(factory);
        }

        public final void clear() {
            this.builder.clear();
        }
    }

}
