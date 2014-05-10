package capnp;

public interface StructFactory<T> {
    public abstract T createFromStructReader(StructReader reader);
}
