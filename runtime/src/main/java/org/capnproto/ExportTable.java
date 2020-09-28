package org.capnproto;

import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Consumer;

abstract class ExportTable<T> implements Iterable<T> {

    final HashMap<Integer, T> slots = new HashMap<>();
    final Queue<Integer> freeIds = new PriorityQueue<>();
    int max = 0;

    protected abstract T newExportable();

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
        if (freeIds.isEmpty()) {
            var id = max;
            max++;
            var value = newExportable();
            slots.put(id, value);
            return value;
        } else {
            var id = freeIds.remove();
            var value = newExportable();
            slots.put(id, value);
            return value;
        }
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

