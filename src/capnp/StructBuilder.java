package org.capnproto;

public final class StructBuilder {
    public SegmentBuilder segment;
    public int data; //byte offset to data section
    public int pointers; // word offset of pointer section
    public int dataSize; // in bits
    public short pointerCount;
    public byte bit0Offset;

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


    public int getIntField(int offset) {
        return this.segment.ptr.getInt(this.data + offset * 4);
    }

    public void setIntField(int offset, int value) {
        this.segment.ptr.putInt(this.data + offset * 4, value);
    }

}
