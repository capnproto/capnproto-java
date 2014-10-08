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

    public final void setList(ListReader value) {
        WireHelpers.setListPointer(this.segment, this.pointer, value);
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
