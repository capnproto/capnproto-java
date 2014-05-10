package capnp;


public class StructList {
    public static class Reader<T> {
        public ListReader reader;
        public FromStructReader<T> factory;

        public Reader(ListReader reader, FromStructReader<T> factory) {
            this.reader = reader;
            this.factory = factory;
        }

        public int size() {
            return this.reader.size();
        }

        public T get(int index) {
            return this.factory.fromStructReader(this.reader.getStructElement(index));
        }
    }

}
