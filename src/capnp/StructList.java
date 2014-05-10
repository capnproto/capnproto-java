package capnp;


public class StructList {
    public static class Reader<T> {
        public ListReader reader;
        public StructFactory<T> factory;

        public Reader(ListReader reader, StructFactory<T> factory) {
            this.reader = reader;
            this.factory = factory;
        }

        public int size() {
            return this.reader.size();
        }

        public T get(int index) {
            StructReader sr = this.reader.getStructElement(index);
            return this.factory.createFromStructReader(sr);
        }
    }

}
