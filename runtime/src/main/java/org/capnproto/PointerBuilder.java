package org.capnproto;

public final class PointerBuilder {
    public final SegmentBuilder segment;
    public final int pointer; // word offset

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
        return WireHelpers.getWritableStructPointer(this.pointer, this.segment, size);
    }

    public final ListBuilder getList(byte elementSize) {
        return WireHelpers.getWritableListPointer(this.pointer, this.segment, elementSize);
    }

    public final ListBuilder getStructList(StructSize elementSize) {
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
        throw new Error("unimplemented");
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

    public final void setText(Text.Reader value) {
        WireHelpers.setTextPointer(this.pointer, this.segment, value);
    }

    public final void setData(Data.Reader value) {
        throw new Error("unimplemented");
    }

    public final void clear() {
        WireHelpers.zeroObject(this.segment, this.pointer);
        this.segment.buffer.putLong(this.pointer * 8, 0L);
    }
}
