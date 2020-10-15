package org.capnproto;

import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;

abstract class ImportTable<T> implements Iterable<T> {

    private final HashMap<Integer, T> slots = new HashMap<>();

    protected abstract T newImportable(int id);

    public T put(int id) {
        return this.slots.computeIfAbsent(id, key -> newImportable(id));
    }

    public T find(int id) {
        return slots.get(id);
    }

    public T erase(int id, T entry) {
        var removed = slots.remove(id, entry);
        assert removed;
        return entry;
    }

    public T erase(int id) {
        return slots.remove(id);
    }

    @Override
    public Iterator<T> iterator() {
        return slots.values().iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        slots.values().forEach(action);
    }
}
