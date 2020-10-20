package org.capnproto;

import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Consumer;

abstract class ExportTable<T> implements Iterable<T> {

    private final HashMap<Integer, T> slots = new HashMap<>();
    private final Queue<Integer> freeIds = new PriorityQueue<>();
    private int max = 0;

    abstract T newExportable(int id);

    public T find(int id) {
        return slots.get(id);
    }

    public T erase(int id, T entry) {
        var value = slots.get(id);
        if (value == entry) {
            freeIds.add(id);
            return slots.remove(id);
        } else {
            return null;
        }
    }

    public T next() {
        int id = freeIds.isEmpty() ? max++ : freeIds.remove();
        var value = newExportable(id);
        var prev = slots.put(id, value);
        assert prev == null;
        return value;
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

