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

    public final StructBuilder initStruct(StructSize size) {
        return WireHelpers.initStructPointer(this.pointer, this.segment, size);
    }

    public final ListBuilder initList(byte elementSize, int elementCount) {
        throw new Error("unimplemented");
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

}
