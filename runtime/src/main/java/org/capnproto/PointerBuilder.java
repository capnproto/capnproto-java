package org.capnproto;

public final class PointerBuilder {
    final SegmentBuilder segment;
    final int pointer; // word offset

    public PointerBuilder(SegmentBuilder segment, int pointer) {
        this.segment = segment;
        this.pointer = pointer;
    }

    public static PointerBuilder getRoot(SegmentBuilder segment, int location) {
        return new PointerBuilder(segment, location);
    }

    public final boolean isNull() {
        return this.segment.buffer.getLong(this.pointer) == 0;
    }

    public final StructBuilder getStruct(StructSize size) {
        return WireHelpers.getWritableStructPointer(this.pointer, this.segment, size, null, 0);
    }

    public final StructBuilder getStruct(StructSize size, SegmentReader defaultBuffer, int defaultOffset) {
        return WireHelpers.getWritableStructPointer(this.pointer, this.segment, size, defaultBuffer, defaultOffset);
    }

    public final ListBuilder getList(byte elementSize, SegmentReader defaultBuffer, int defaultOffset) {
        return WireHelpers.getWritableListPointer(this.pointer, this.segment, elementSize);
    }

    public final ListBuilder getStructList(StructSize elementSize, SegmentReader defaultSegment, int defaultOffset) {
        throw new Error("unimplemented");
    }

    public final Text.Builder getText() {
        return WireHelpers.getWritableTextPointer(
            this.pointer, this.segment, null, 0, 0);
    }

    public Text.Builder getText(java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
        return WireHelpers.getWritableTextPointer(this.pointer,
                                                  this.segment,
                                                  defaultBuffer,
                                                  defaultOffset,
                                                  defaultSize);
    }

    public final Data.Builder getData() {
        return WireHelpers.getWritableDataPointer(this.pointer,
                                                  this.segment,
                                                  null, 0, 0);
    }

    public Data.Builder getData(java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
        return WireHelpers.getWritableDataPointer(this.pointer,
                                                  this.segment,
                                                  defaultBuffer,
                                                  defaultOffset,
                                                  defaultSize);
    }

    public final StructBuilder initStruct(StructSize size) {
        return WireHelpers.initStructPointer(this.pointer, this.segment, size);
    }

    public final ListBuilder initList(byte elementSize, int elementCount) {
        return WireHelpers.initListPointer(this.pointer, this.segment, elementCount, elementSize);
    }

    public final ListBuilder initStructList(int elementCount, StructSize elementSize) {
        return WireHelpers.initStructListPointer(this.pointer, this.segment, elementCount, elementSize);
    }

    public final void setStruct(StructReader value) {
        WireHelpers.setStructPointer(this.segment, this.pointer, value);
    }

    public final void setText(Text.Reader value) {
        WireHelpers.setTextPointer(this.pointer, this.segment, value);
    }

    public final void setData(Data.Reader value) {
        WireHelpers.setDataPointer(this.pointer, this.segment, value);
    }

    public final void clear() {
        WireHelpers.zeroObject(this.segment, this.pointer);
        this.segment.buffer.putLong(this.pointer * 8, 0L);
    }
}
