package org.capnproto;

public final class PointerBuilder {
    public SegmentBuilder segment;
    public int pointer; // word offset

    public PointerBuilder(SegmentBuilder segment, int pointer) {
        this.segment = segment;
        this.pointer = pointer;
    }

    public final boolean isNull() {
        return this.segment.buffer.getLong(this.pointer) == 0;
    }

    public final ListBuilder initStructList(int elementCount, StructSize elementSize) {
        return WireHelpers.initStructListPointer(this.pointer, this.segment, elementCount, elementSize);
    }

    public final void setText(Text.Reader value) {
        WireHelpers.setTextPointer(this.pointer, this.segment, value);
    }

}
