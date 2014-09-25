package org.capnproto;

public final class ListBuilder {
    final SegmentBuilder segment;
    final int ptr; // byte offset to front of list
    final int elementCount;
    final int step; // in bits
    final int structDataSize; // in bits
    final short structPointerCount;

    public ListBuilder(SegmentBuilder segment, int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount) {
        this.segment = segment;
        this.ptr = ptr;
        this.elementCount = elementCount;
        this.step = step;
        this.structDataSize = structDataSize;
        this.structPointerCount = structPointerCount;
    }

    public int size() {
        return this.elementCount;
    }

    public boolean getBooleanElement(int index) {
        byte b = this.segment.buffer.get(this.ptr + index / 8);
        return (b & (1 << (index % 8))) != 0;
    }

    public byte getByteElement(int index) {
        return this.segment.buffer.get(this.ptr + index);
    }

    public short getShortElement(int index) {
        return this.segment.buffer.getShort(this.ptr + index * 2);
    }

    public int getIntElement(int index) {
        return this.segment.buffer.getInt(this.ptr + index * 4);
    }

    public long getLongElement(int index) {
        return this.segment.buffer.getLong(this.ptr + index * 8);
    }

    public float getFloatElement(int index) {
        return this.segment.buffer.getFloat(this.ptr + index * 4);
    }

    public double getDoubleElement(int index) {
        return this.segment.buffer.getDouble(this.ptr + index * 8);
    }

    public void setBooleanElement(int index, boolean value) {
        int bitOffset = index;
        byte bitnum = (byte)(bitOffset % 8);
        int position = this.ptr + (bitOffset / 8);
        byte oldValue = this.segment.buffer.get(position);
        this.segment.buffer.put(position,
                                (byte)((oldValue & (~(1 << bitnum))) | (( value ? 1 : 0) << bitnum)));
    }

    public void setByteElement(int index, byte value) {
        this.segment.buffer.put(this.ptr + index, value);
    }

    public void setShortElement(int index, short value) {
        this.segment.buffer.putShort(this.ptr + index * 2, value);
    }

    public void setIntElement(int index, int value) {
        this.segment.buffer.putInt(this.ptr + index * 4, value);
    }

    public void setLongElement(int index, long value) {
        this.segment.buffer.putLong(this.ptr + index * 8, value);
    }

    public void setFloatElement(int index, float value) {
        this.segment.buffer.putFloat(this.ptr + index * 4, value);
    }

    public void setDoubleElement(int index, double value) {
        this.segment.buffer.putDouble(this.ptr + index * 8, value);
    }

    public final StructBuilder getStructElement(int index) {
        int indexBit = index * this.step;
        int structData = this.ptr + indexBit / 8 ;
        int structPointers = (structData + (this.structDataSize / 8)) / 8;

        return new StructBuilder(this.segment,
                                 structData,
                                 structPointers,
                                 this.structDataSize,
                                 this.structPointerCount,
                                 (byte)(indexBit % 8));
    }


    public final PointerBuilder getPointerElement(int index) {
        return new PointerBuilder(
            this.segment,
            (this.ptr + (index * this.step / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD);
    }

}
