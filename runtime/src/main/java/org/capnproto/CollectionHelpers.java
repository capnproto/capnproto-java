package org.capnproto;


import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class CollectionHelpers {
    private CollectionHelpers() {}


    public static <T, Capnp extends StructBuilder> void serializeCollection(
            Collection<T> items,
            Function<Integer, StructList.Builder<Capnp>> getListBuilder,
            BiConsumer<T, Capnp> serializeItem) {
        StructList.Builder<Capnp> listBuilder = getListBuilder.apply(items.size());

        int i = 0;
        for (T item : items) {
            serializeItem.accept(item, listBuilder.get(i));
            ++i;
        }
    }

    public static <T, Capnp extends StructBuilder> void serializeCollection(
            T[] items,
            Function<Integer, StructList.Builder<Capnp>> getListBuilder,
            BiConsumer<T, Capnp> serializeItem) {
        StructList.Builder<Capnp> listBuilder = getListBuilder.apply(items.length);

        int i = 0;
        for (T item : items) {
            serializeItem.accept(item, listBuilder.get(i));
            ++i;
        }
    }

    public static void serializeLongCollection(
            Collection<Long> items,
            Function<Integer, PrimitiveList.Long.Builder> getListBuilder) {
        PrimitiveList.Long.Builder listBuilder = getListBuilder.apply(items.size());

        int i = 0;
        for (Long item : items) {
            listBuilder.set(i, item);
            ++i;
        }
    }

    public static <T> void serializeLongCollection(
            Collection<T> items,
            Function<Integer, PrimitiveList.Long.Builder> getListBuilder,
            Function<T, Long> transform) {
        PrimitiveList.Long.Builder listBuilder = getListBuilder.apply(items.size());

        int i = 0;
        for (T item : items) {
            listBuilder.set(i, transform.apply(item));
            ++i;
        }
    }

    public static void serializeLongCollection(
            long[] items,
            Function<Integer, PrimitiveList.Long.Builder> getListBuilder) {
        PrimitiveList.Long.Builder listBuilder = getListBuilder.apply(items.length);

        int i = 0;
        for (long item : items) {
            listBuilder.set(i, item);
            ++i;
        }
    }

    public static void serializeIntCollection(
            Collection<Integer> items,
            Function<Integer, PrimitiveList.Int.Builder> getListBuilder) {
        PrimitiveList.Int.Builder listBuilder = getListBuilder.apply(items.size());

        int i = 0;
        for (Integer item : items) {
            listBuilder.set(i, item);
            ++i;
        }
    }

    public static void serializeIntCollection(
            int[] ints,
            java.util.function.Function<Integer, PrimitiveList.Int.Builder> getListBuilder) {
        PrimitiveList.Int.Builder listBuilder = getListBuilder.apply(ints.length);

        int i = 0;
        for (int item : ints) {
            listBuilder.set(i, item);
            ++i;
        }
    }

    public static void serializeIntCollection(
            byte[] items,
            java.util.function.Function<Integer, PrimitiveList.Int.Builder> getListBuilder) {
        PrimitiveList.Int.Builder listBuilder = getListBuilder.apply(items.length);

        int i = 0;
        for (byte item : items) {
            listBuilder.set(i, item);
            ++i;
        }
    }

    public static <T> void serializeIntCollection(
            T[] items,
            java.util.function.Function<Integer, PrimitiveList.Int.Builder> getListBuilder,
            java.util.function.Function<T, Integer> transform) {
        PrimitiveList.Int.Builder listBuilder = getListBuilder.apply(items.length);

        int i = 0;
        for (T item : items) {
            listBuilder.set(i, transform.apply(item));
            ++i;
        }
    }

    public static <T> void serializeIntCollection(
            Collection<T> collection,
            java.util.function.Function<Integer, PrimitiveList.Int.Builder> getListBuilder,
            java.util.function.Function<T, Integer> transform) {
        PrimitiveList.Int.Builder listBuilder = getListBuilder.apply(collection.size());

        int i = 0;
        for (T item : collection) {
            listBuilder.set(i, transform.apply(item));
            ++i;
        }
    }
}

