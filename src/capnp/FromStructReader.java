package capnp;

public interface FromStructReader<T> {
    public abstract T fromStructReader(StructReader reader);
}
