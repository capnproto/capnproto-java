package org.capnproto;

public final class AnyPointer {

    public final static class Reader {
        public final PointerReader reader;

        public Reader(PointerReader reader) {
            this.reader = reader;
        }

        public final <T> T getAs(FromPointerReader<T> factory) {
            return factory.fromPointerReader(this.reader.segment, this.reader.pointer, null, 0, this.reader.nestingLimit);
        }
    }

    public static final class Builder {
        public final PointerBuilder builder;

        public Builder(PointerBuilder builder) {
            this.builder = builder;
        }

        public final <T> T initAs(InitFromPointerBuilder<T> factory) {
            return factory.initFromPointerBuilder(this.builder.segment, this.builder.pointer);
        }

        public final <T> T initAs(InitSizedFromPointerBuilder<T> factory, int elementCount) {
            return factory.initSizedFromPointerBuilder(this.builder.segment, this.builder.pointer, elementCount);
        }

        public final void clear() {
            this.builder.clear();
        }
    }

}
