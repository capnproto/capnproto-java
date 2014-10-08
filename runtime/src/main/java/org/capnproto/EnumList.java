package org.capnproto;

public class EnumList {
    static <T> T clampOrdinal(T values[], short ordinal) {
        int index = ordinal;
        if (ordinal < 0 || ordinal >= values.length) {
            index = values.length - 1;
        }
        return values[index];
    }

    public static final class Factory<T extends java.lang.Enum> implements ListFactory<Builder<T>, Reader<T>>{
        public final T values[];

        public Factory(T values[]) {
            this.values = values;
        }
        public final Reader<T> constructReader(SegmentReader segment,
                                               int ptr,
                                               int elementCount, int step,
                                               int structDataSize, short structPointerCount,
                                               int nestingLimit) {
            return new Reader<T>(values,
                                 segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }

        public final Builder<T> constructBuilder(SegmentBuilder segment,
                                                 int ptr,
                                                 int elementCount, int step,
                                                 int structDataSize, short structPointerCount) {
            return new Builder<T> (values, segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }

        public final Reader<T> fromPointerReader(PointerReader reader, SegmentReader defaultSegment, int defaultOffset) {
            return WireHelpers.readListPointer(this,
                                               reader.segment,
                                               reader.pointer,
                                               defaultSegment,
                                               defaultOffset,
                                               FieldSize.TWO_BYTES,
                                               reader.nestingLimit);
        }

        public final Builder<T> fromPointerBuilder(PointerBuilder builder, SegmentReader defaultSegment, int defaultOffset) {
            return WireHelpers.getWritableListPointer(this,
                                                      builder.pointer,
                                                      builder.segment,
                                                      FieldSize.TWO_BYTES,
                                                      defaultSegment,
                                                      defaultOffset);
        }

        public final Builder<T> initSizedFromPointerBuilder(PointerBuilder builder, int elementCount) {
            return WireHelpers.initListPointer(this, builder.pointer, builder.segment, elementCount, FieldSize.TWO_BYTES);
        }
    }

    public static final class Reader<T extends java.lang.Enum> extends ListReader {
        public final T values[];

        public Reader(T values[],
                      SegmentReader segment,
                      int ptr,
                      int elementCount, int step,
                      int structDataSize, short structPointerCount,
                      int nestingLimit) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            this.values = values;
        }

        public T get(int index) {
            return clampOrdinal(this.values, _getShortElement(index));
        }
    }

    public static final class Builder<T extends java.lang.Enum> extends ListBuilder {
        public final T values[];

        public Builder(T values[],
                       SegmentBuilder segment,
                       int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            this.values = values;
        }

        public T get(int index) {
            return clampOrdinal(this.values, _getShortElement(index));
        }

        public void set(int index, T value) {
            _setShortElement(index, (short)value.ordinal());
        }
    }
}
