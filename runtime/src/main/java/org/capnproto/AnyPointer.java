package org.capnproto;

public final class AnyPointer {

    public final static class Reader {
        public final PointerReader reader;

        public Reader(PointerReader reader) {
            this.reader = reader;
        }

        public <T> T getAsStruct(FromStructReader<T> factory) {
            return factory.fromStructReader(this.reader.getStruct());
        }
    }

}
