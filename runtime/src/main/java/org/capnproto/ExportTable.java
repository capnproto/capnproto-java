package org.capnproto;

import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Consumer;

class ExportTable<T> implements Iterable<T> {

    final HashMap<Integer, T> slots = new HashMap<>();
    final Queue<Integer> freeIds = new PriorityQueue<>();
    int max = 0;

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

    public int next(T value) {
        if (freeIds.isEmpty()) {
            var id = max;
            max++;
            slots.put(id, value);
            return id;
        } else {
            var id = freeIds.remove();
            slots.put(id, value);
            return id;
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

