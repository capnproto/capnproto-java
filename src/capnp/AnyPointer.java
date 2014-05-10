package capnp;

public class AnyPointer {

    public static class Reader {
        public PointerReader reader;

        public Reader(PointerReader reader) {
            this.reader = reader;
        }

        public <T> T getAsStruct(FromStructReader<T> factory) {
            return factory.fromStructReader(this.reader.getStruct());
        }
    }

}
