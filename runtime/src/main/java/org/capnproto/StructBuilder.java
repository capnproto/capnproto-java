package org.capnproto;

public final class StructBuilder {
    public final SegmentBuilder segment;
    public final int data; //byte offset to data section
    public final int pointers; // word offset of pointer section
    public final int dataSize; // in bits
    public final short pointerCount;
    public final byte bit0Offset;

    public StructBuilder(SegmentBuilder segment, int data,
                         int pointers, int dataSize, short pointerCount,
                         byte bit0Offset) {
        this.segment = segment;
        this.data = data;
        this.pointers = pointers;
        this.dataSize = dataSize;
        this.pointerCount = pointerCount;
        this.bit0Offset = bit0Offset;
    }

    public final int getShortField(int offset) {
        return this.segment.buffer.getShort(this.data + offset * 2);
    }

    public final void setShortField(int offset, short value) {
        this.segment.buffer.putShort(this.data + offset * 2, value);
    }

    public final int getIntField(int offset) {
        return this.segment.buffer.getInt(this.data + offset * 4);
    }

    public final void setIntField(int offset, int value) {
        this.segment.buffer.putInt(this.data + offset * 4, value);
    }

    public final PointerBuilder getPointerField(int index) {
        return new PointerBuilder(this.segment, this.pointers + index);
    }

}
