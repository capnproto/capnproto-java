package org.capnproto;

public final class StructBuilder {
    public final SegmentBuilder segment;
    public final int data; // byte offset to data section
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

    public final StructReader asReader() {
        return new StructReader(this.segment,
                                this.data, this.pointers, this.dataSize,
                                this.pointerCount, this.bit0Offset,
                                0x7fffffff);
    }

    public final boolean getBooleanField(int offset) {
        int bitOffset = (offset == 0 ? this.bit0Offset : offset);
        int position = this.data + (bitOffset / 8);
        return (this.segment.buffer.get(position) & (1 << (bitOffset % 8))) != 0;
    }

    public final void setBooleanField(int offset, boolean value) {
        int bitOffset = offset;
        if (offset == 0) { bitOffset = this.bit0Offset; }
        byte bitnum = (byte)(bitOffset % 8);
        int position = this.data + (bitOffset / 8);
        byte oldValue = this.segment.buffer.get(position);
        this.segment.buffer.put(position,
                                (byte)((oldValue & (~(1 << bitnum))) | (( value ? 1 : 0) << bitnum)));
    }

    public final byte getByteField(int offset) {
        return this.segment.buffer.get(this.data + offset);
    }

    public final void setByteField(int offset, byte value) {
        this.segment.buffer.put(this.data + offset, value);
    }

    public final short getShortField(int offset) {
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

    public final long getLongField(int offset) {
        return this.segment.buffer.getLong(this.data + offset * 8);
    }

    public final void setLongField(int offset, long value) {
        this.segment.buffer.putLong(this.data + offset * 8, value);
    }

    public final float getFloatField(int offset) {
        return this.segment.buffer.getFloat(this.data + offset * 4);
    }

    public final void setFloatField(int offset, float value) {
        this.segment.buffer.putFloat(this.data + offset * 4, value);
    }

    public final double getDoubleField(int offset) {
        return this.segment.buffer.getDouble(this.data + offset * 8);
    }

    public final void setDoubleField(int offset, double value) {
        this.segment.buffer.putDouble(this.data + offset * 8, value);
    }


    public final PointerBuilder getPointerField(int index) {
        return new PointerBuilder(this.segment, this.pointers + index);
    }

}
