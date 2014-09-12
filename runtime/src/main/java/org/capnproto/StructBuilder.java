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

    public final boolean getBooleanField(int offset, boolean mask) {
        return this.getBooleanField(offset) ^ mask;
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

    public final void setBooleanField(int offset, boolean value, boolean mask) {
        this.setBooleanField(offset, value ^ mask);
    }

    public final byte getByteField(int offset) {
        return this.segment.buffer.get(this.data + offset);
    }

    public final byte getByteField(int offset, byte mask) {
        return (byte)(this.getByteField(offset) ^ mask);
    }

    public final void setByteField(int offset, byte value) {
        this.segment.buffer.put(this.data + offset, value);
    }

    public final void setByteField(int offset, byte value, byte mask) {
        this.setByteField(offset, (byte) (value ^ mask));
    }

    public final short getShortField(int offset) {
        return this.segment.buffer.getShort(this.data + offset * 2);
    }

    public final short getShortField(int offset, short mask) {
        return (short)(this.getShortField(offset) ^ mask);
    }

    public final void setShortField(int offset, short value) {
        this.segment.buffer.putShort(this.data + offset * 2, value);
    }

    public final void setShortField(int offset, short value, short mask) {
        this.setShortField(offset, (short)(value ^ mask));
    }

    public final int getIntField(int offset) {
        return this.segment.buffer.getInt(this.data + offset * 4);
    }

    public final int getIntField(int offset, int mask) {
        return this.getIntField(offset) ^ mask;
    }

    public final void setIntField(int offset, int value) {
        this.segment.buffer.putInt(this.data + offset * 4, value);
    }

    public final void setIntField(int offset, int value, int mask) {
        this.setIntField(offset, value ^ mask);
    }

    public final long getLongField(int offset) {
        return this.segment.buffer.getLong(this.data + offset * 8);
    }

    public final long getLongField(int offset, long mask) {
        return this.getLongField(offset) ^ mask;
    }

    public final void setLongField(int offset, long value) {
        this.segment.buffer.putLong(this.data + offset * 8, value);
    }

    public final void setLongField(int offset, long value, long mask) {
        this.setLongField(offset, value ^ mask);
    }

    public final float getFloatField(int offset) {
        return this.segment.buffer.getFloat(this.data + offset * 4);
    }

    public final float getFloatField(int offset, int mask) {
        return Float.intBitsToFloat(
            this.segment.buffer.getInt(this.data + offset * 4) ^ mask);
    }

    public final void setFloatField(int offset, float value) {
        this.segment.buffer.putFloat(this.data + offset * 4, value);
    }

    public final void setFloatField(int offset, float value, int mask) {
        this.segment.buffer.putInt(this.data + offset * 4,
                                   Float.floatToIntBits(value) ^ mask);
    }

    public final double getDoubleField(int offset) {
        return this.segment.buffer.getDouble(this.data + offset * 8);
    }

    public final double getDoubleField(int offset, long mask) {
        return Double.longBitsToDouble(
            this.segment.buffer.getLong(this.data + offset * 8) ^ mask);
    }

    public final void setDoubleField(int offset, double value) {
        this.segment.buffer.putDouble(this.data + offset * 8, value);
    }

    public final void setDoubleField(int offset, double value, long mask) {
        this.segment.buffer.putLong(this.data + offset * 8,
                                    Double.doubleToLongBits(value) ^ mask);
    }

    public final PointerBuilder getPointerField(int index) {
        return new PointerBuilder(this.segment, this.pointers + index);
    }

}
